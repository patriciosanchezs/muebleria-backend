package com.muebleria.service;

import com.muebleria.dto.AbonoRequest;
import com.muebleria.dto.PaymentRequest;
import com.muebleria.dto.SaleItemRequest;
import com.muebleria.dto.SaleRequest;
import com.muebleria.exception.BadRequestException;
import com.muebleria.exception.ResourceNotFoundException;
import com.muebleria.model.CanalVenta;
import com.muebleria.model.Payment;
import com.muebleria.model.Product;
import com.muebleria.model.Role;
import com.muebleria.model.Sale;
import com.muebleria.model.SaleItem;
import com.muebleria.model.User;
import com.muebleria.repository.SaleRepository;
import com.muebleria.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleService {
    
    private final SaleRepository saleRepository;
    private final ProductService productService;
    private final CommissionService commissionService;
    private final UserRepository userRepository;
    private final AuthHelper authHelper;
    private final LocalService localService;
    
    /**
     * Crea una nueva venta, valida stock por local y descuenta productos del local correcto.
     * Soporta ventas normales y encargos.
     */
    @Transactional
    public Sale createSale(SaleRequest request, String vendedor) {
        // 1. Validar que el local esté especificado
        if (request.getLocal() == null || request.getLocal().isEmpty()) {
            throw new BadRequestException("Debe especificar el local de la venta");
        }

        // Validar que el local ID existe y está activo
        localService.validateActiveLocalId(request.getLocal());
        String localId = request.getLocal();

        // Validar y convertir canal de venta
        CanalVenta canalVenta;
        try {
            canalVenta = CanalVenta.valueOf(request.getCanalVenta());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Canal de venta inválido: " + request.getCanalVenta());
        }

        // Determinar si es un encargo
        boolean esEncargo = "ENCARGO".equals(request.getTipoVenta());

        // Si es encargo, validar que el usuario tenga los roles permitidos
        User currentUser = userRepository.findByUsername(vendedor)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (esEncargo) {
            boolean rolPermitido = currentUser.getRole() == Role.ADMINISTRADOR ||
                                   currentUser.getRole() == Role.ADMIN_LOCAL ||
                                   currentUser.getRole() == Role.ENCARGADO_LOCAL;
            if (!rolPermitido) {
                throw new BadRequestException("Solo ADMINISTRADOR, ADMIN_LOCAL y ENCARGADO_LOCAL pueden crear encargos");
            }
        }

        // 2. Validar que todos los productos tengan stock suficiente en el local especificado (solo para ventas normales)
        if (!esEncargo) {
            validateStockByLocal(request.getItems(), localId);
        }

        // 3. Validar pagos
        validatePayments(request.getPayments());

        // Determinar el dueño de la venta: vendedor asignado o usuario de sesión
        String vendedorFinal = request.getVendedorAsignado() != null && !request.getVendedorAsignado().isEmpty()
                ? request.getVendedorAsignado()
                : vendedor;

        // Obtener usuario actual (quien registra la venta) para registrar quien aplicó descuentos

        // Validar que si es DESPACHO, el monto del flete esté presente (puede ser 0)
        if ("DESPACHO".equals(request.getTipoEntrega()) && request.getMontoFlete() == null) {
            throw new BadRequestException("El monto del flete es requerido para despachos a domicilio");
        }
        
        // Validar datos del cliente para DESPACHO
        if ("DESPACHO".equals(request.getTipoEntrega())) {
            if (request.getClienteTelefono() == null || request.getClienteTelefono().isEmpty()) {
                throw new BadRequestException("El teléfono del cliente es requerido para despachos a domicilio");
            }
            if (request.getClienteDireccion() == null || request.getClienteDireccion().isEmpty()) {
                throw new BadRequestException("La dirección del cliente es requerida para despachos a domicilio");
            }
        }

        // 3. Crear items de la venta y calcular total
        List<SaleItem> saleItems = new ArrayList<>();
        double totalCLP = 0.0;
        
        for (SaleItemRequest itemRequest : request.getItems()) {
            Product product = productService.getProductById(itemRequest.getProductId());
            
            // Validar y aplicar descuento si existe
            double descuento = itemRequest.getDescuento() != null ? itemRequest.getDescuento() : 0.0;
            
            // Validación: el descuento no puede exceder el precio del producto
            if (descuento > product.getPrecio()) {
                throw new BadRequestException(
                    String.format("El descuento ($%,.0f) no puede ser mayor al precio del producto '%s' ($%,.0f)", 
                        descuento, product.getNombre(), product.getPrecio())
                );
            }
            
            // Validación: el descuento no puede ser negativo
            if (descuento < 0) {
                throw new BadRequestException("El descuento no puede ser negativo");
            }
            
            // Calcular precio con descuento
            double precioConDescuento = product.getPrecio() - descuento;
            double subtotal = precioConDescuento * itemRequest.getCantidad();
            
            SaleItem.SaleItemBuilder saleItemBuilder = SaleItem.builder()
                    .productId(product.getId())
                    .productName(product.getNombre())
                    .cantidad(itemRequest.getCantidad())
                    .precioUnitario(product.getPrecio())
                    .descuento(descuento)
                    .subtotal(subtotal);
            
            // Si hay descuento, registrar quién lo aplicó
            if (descuento > 0) {
                saleItemBuilder.descuentoAplicadoPor(currentUser.getUsername());
            }
            
            SaleItem saleItem = saleItemBuilder.build();
            
            saleItems.add(saleItem);
            totalCLP += subtotal;
        }

        // Agregar monto del flete al total si existe
        if (request.getMontoFlete() != null) {
            totalCLP += request.getMontoFlete();
        }

        // 4. Validar que la suma de pagos sea igual al total (solo para ventas normales)
        if (!esEncargo) {
            validatePaymentTotal(request.getPayments(), totalCLP);
        }
        
// 5. Convert PaymentRequest to Payment
        List<Payment> payments = request.getPayments().stream()
                .map(pr -> Payment.builder()
                        .paymentMethod(pr.getPaymentMethod())
                        .amount(pr.getAmount())
                        .paymentDate(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());
        
        // 6. Crear la venta
        Sale.SaleBuilder saleBuilder = Sale.builder()
                .items(saleItems)
                .totalCLP(totalCLP)
                .payments(payments)  // Payment list
                .vendedor(vendedorFinal)  // Vendedor asignado o usuario de sesión
                .clienteNombre(request.getClienteNombre())
                .clienteDireccion(request.getClienteDireccion())
                .clienteCorreo(request.getClienteCorreo())
                .clienteTelefono(request.getClienteTelefono())
                .tipoEntrega(request.getTipoEntrega())
                .localId(localId)
                .canalVenta(canalVenta)
                .notas(request.getNotas())
                .fechaVenta(LocalDateTime.now())
                .tipoVenta(esEncargo ? "ENCARGO" : "NORMAL");

        // Campos para encargos
        if (esEncargo) {
            double totalAbonado = payments.stream().mapToDouble(Payment::getAmount).sum();
            saleBuilder.totalAbonado(totalAbonado);
            saleBuilder.saldoPendiente(totalCLP - totalAbonado);
            saleBuilder.estadoPago(totalAbonado >= totalCLP ? "PAGADO_COMPLETO" : (totalAbonado > 0 ? "ABONADO_PARCIAL" : "PENDIENTE"));
        }
        
        // Determinar estado de aprobación según el rol de QUIEN REGISTRA la venta (no del vendedor asignado)
        // Si quien registra es ADMIN, ADMIN_LOCAL o ENCARGADO_LOCAL → APROBADA automáticamente
        // Si quien registra es VENDEDOR o VENDEDOR_SIN_COMISION → PENDIENTE_APROBACION
        if ("VENDEDOR".equals(currentUser.getRole().name()) || "VENDEDOR_SIN_COMISION".equals(currentUser.getRole().name())) {
            // Vendedores (con o sin comisión) crean ventas en estado PENDIENTE_APROBACION
            saleBuilder.estadoAprobacion("PENDIENTE_APROBACION");
        } else {
            // Admins, Admin Local y Encargado Local crean ventas ya APROBADAS
            saleBuilder.estadoAprobacion("APROBADA")
                    .fechaAprobacion(LocalDateTime.now());
        }
        
        // Siempre registrar quién registró/aprobó esta venta (usuario de sesión)
        saleBuilder.aprobadoPor(vendedor);
        
        // Si el tipo de entrega es ENTREGADO, marcar como entregado inmediatamente
        if ("ENTREGADO".equals(request.getTipoEntrega())) {
            saleBuilder.estadoEntrega("ENTREGADO");
        } else if ("DESPACHO".equals(request.getTipoEntrega())) {
            saleBuilder.estadoEntrega("POR_ENTREGAR");
            // Parsear fecha de despacho si se proporcionó
            if (request.getFechaDespacho() != null && !request.getFechaDespacho().isEmpty()) {
                // Convertir fecha simple (yyyy-MM-dd) a LocalDateTime al inicio del día
                saleBuilder.fechaDespacho(
                    java.time.LocalDate.parse(request.getFechaDespacho()).atStartOfDay()
                );
            }
            // Guardar monto del flete (puede ser 0)
            if (request.getMontoFlete() != null) {
                saleBuilder.montoFlete(request.getMontoFlete());
            }
        }
        
        Sale sale = saleBuilder.build();
        
        Sale savedSale = saleRepository.save(sale);

        // 5. Descontar stock solo si la venta está APROBADA y NO es encargo
        // Para encargos, el stock se descuenta cuando se pague completo
        if ("APROBADA".equals(savedSale.getEstadoAprobacion()) && !esEncargo) {
            for (SaleItemRequest itemRequest : request.getItems()) {
                productService.confirmarReserva(itemRequest.getProductId(), localId, itemRequest.getCantidad());
            }
        }

        // 6. Crear comisión solo si la venta está APROBADA o es un encargo
        // Para encargos, la comisión se genera al crear el encargo (según requerimiento)
        if ("APROBADA".equals(savedSale.getEstadoAprobacion()) || esEncargo) {
            try {
                // Determinar quién recibe la comisión de vendedor
                String vendedorParaComision = request.getVendedorAsignado() != null && !request.getVendedorAsignado().isEmpty()
                        ? request.getVendedorAsignado()  // Si hay vendedor asignado, usar ese
                        : vendedor;  // Si no, usar quien registró la venta

                // Validar que el vendedor para comisión exista y sea VENDEDOR o ENCARGADO_LOCAL
                User comissionUser = userRepository.findByUsername(vendedorParaComision).orElse(null);

                // Crear comisión solo si:
                // 1. Hay un vendedor asignado explícitamente, O
                // 2. Quien registra la venta es VENDEDOR o ENCARGADO_LOCAL (sin asignación)
                boolean shouldCreateCommission = false;

                if (request.getVendedorAsignado() != null && !request.getVendedorAsignado().isEmpty()) {
                    // Caso 1: Hay vendedor asignado - crear comisión solo si es VENDEDOR o ENCARGADO_LOCAL (NO para VENDEDOR_SIN_COMISION)
                    if (comissionUser != null &&
                        ("VENDEDOR".equals(comissionUser.getRole().name()) || "ENCARGADO_LOCAL".equals(comissionUser.getRole().name()))) {
                        shouldCreateCommission = true;
                    }
                } else {
                    // Caso 2: No hay vendedor asignado - crear comisión solo si quien registra es VENDEDOR o ENCARGADO_LOCAL (NO para VENDEDOR_SIN_COMISION)
                    if (currentUser != null &&
                        ("VENDEDOR".equals(currentUser.getRole().name()) || "ENCARGADO_LOCAL".equals(currentUser.getRole().name()))) {
                        shouldCreateCommission = true;
                    }
                }

                if (shouldCreateCommission && comissionUser != null) {
                    commissionService.createCommission(savedSale, vendedorParaComision);
                }

                // Crear comisiones para todos los ADMIN_LOCAL que tienen comisión habilitada en este local
                createAdminLocalCommissions(savedSale);
            } catch (Exception e) {
                // Si falla la creación de comisión, no afecta la venta
                System.err.println("Error creando comisión: " + e.getMessage());
            }
        }
        
        return savedSale;
    }
    
    /**
     * Crea comisiones para todos los ADMIN_LOCAL que tienen habilitado el local de la venta.
     * Reglas de comisión:
     * - Producto < 140.000 CLP → Comisión = 5.000 CLP
     * - Producto >= 140.000 CLP → Comisión = 10.000 CLP
     */
    private void createAdminLocalCommissions(Sale sale) {
        // Buscar todos los ADMIN_LOCAL que tienen este local en su lista de comisiones
        List<User> adminLocales = userRepository.findAll().stream()
                .filter(u -> u.getRole().name().equals("ADMIN_LOCAL"))
                .filter(u -> u.getLocalesConComisionIds() != null && u.getLocalesConComisionIds().contains(sale.getLocalId()))
                .collect(Collectors.toList());
        
        // Para cada ADMIN_LOCAL, crear comisiones
        for (User adminLocal : adminLocales) {
            commissionService.createAdminLocalCommission(sale, adminLocal.getId(), adminLocal.getUsername());
        }
    }
    
    /**
     * Valida que haya stock suficiente en un local específico para todos los productos.
     */
    private void validateStockByLocal(List<SaleItemRequest> items, String localId) {
        List<String> errors = new ArrayList<>();
        
        for (SaleItemRequest item : items) {
            try {
                Product product = productService.getProductById(item.getProductId());
                
                // Validar que el producto pertenece al local de la venta
                if (!localId.equals(product.getLocalId())) {
                    errors.add(String.format(
                        "Producto '%s' no pertenece al local con ID %s",
                        product.getNombre(), localId
                    ));
                    continue;
                }
                
                if (!product.isDisponible()) {
                    errors.add(String.format("Producto '%s' no está disponible", product.getNombre()));
                    continue;
                }
                
                Integer stockEnLocal = product.getStock();
                if (stockEnLocal < item.getCantidad()) {
                    errors.add(String.format(
                        "Stock insuficiente para '%s' en local %s. Disponible: %d, Requerido: %d",
                        product.getNombre(), localId, stockEnLocal, item.getCantidad()
                    ));
                }
            } catch (ResourceNotFoundException e) {
                errors.add(String.format("Producto con ID '%s' no encontrado", item.getProductId()));
            }
        }
        
        if (!errors.isEmpty()) {
            throw new BadRequestException("Errores en la venta: " + String.join("; ", errors));
        }
    }
    
    /**
     * Obtiene todas las ventas (filtradas por locales autorizados).
     */
    public List<Sale> getAllSales(org.springframework.security.core.Authentication authentication) {
        List<Sale> allSales = saleRepository.findAll();
        return filterByAuthorizedLocales(allSales, authentication);
    }
    
    /**
     * Obtiene ventas con filtros aplicados (filtradas por locales autorizados).
     * Por defecto devuelve solo las ventas del día actual si no se especifica allTime=true.
     */
    public List<Sale> getSalesWithFilters(
            String local, 
            String vendedor, 
            String metodoPago, 
            String estadoEntrega,
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Boolean todayOnly,
            Boolean allTime,
            org.springframework.security.core.Authentication authentication) {
        
        List<Sale> sales;
        
        // Si allTime es true, traer todas las ventas
        if (allTime != null && allTime) {
            sales = saleRepository.findAll();
        }
        // Si todayOnly es true o no hay fechas especificadas, usar ventas del día
        else if ((todayOnly != null && todayOnly) || (startDate == null && endDate == null)) {
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(23, 59, 59);
            sales = saleRepository.findByFechaVentaBetween(startOfDay, endOfDay);
        }
        // Si hay rango de fechas, usarlo
        else if (startDate != null || endDate != null) {
            LocalDateTime start = startDate != null ? startDate : LocalDateTime.of(2000, 1, 1, 0, 0);
            LocalDateTime end = endDate != null ? endDate : LocalDateTime.now().plusDays(1);
            sales = saleRepository.findByFechaVentaBetween(start, end);
        }
        // Por defecto, ventas del día actual
        else {
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(23, 59, 59);
            sales = saleRepository.findByFechaVentaBetween(startOfDay, endOfDay);
        }
        
        // Aplicar filtros adicionales
        if (local != null && !local.isEmpty() && !local.equals("ALL")) {
            final String localId = local;
            sales = sales.stream()
                    .filter(sale -> localId.equals(sale.getLocalId()))
                    .collect(Collectors.toList());
        }
        
        if (vendedor != null && !vendedor.isEmpty() && !vendedor.equals("ALL")) {
            sales = sales.stream()
                    .filter(sale -> vendedor.equals(sale.getVendedor()))
                    .collect(Collectors.toList());
        }
        
        if (metodoPago != null && !metodoPago.isEmpty() && !metodoPago.equals("ALL")) {
            sales = sales.stream()
                    .filter(sale -> sale.getPayments() != null && sale.getPayments().stream()
                            .anyMatch(payment -> metodoPago.equals(payment.getPaymentMethod())))
                    .collect(Collectors.toList());
        }
        
        if (estadoEntrega != null && !estadoEntrega.isEmpty() && !estadoEntrega.equals("ALL")) {
            sales = sales.stream()
                    .filter(sale -> estadoEntrega.equals(sale.getEstadoEntrega()))
                    .collect(Collectors.toList());
        }
        
        // Filtrar por locales autorizados
        sales = filterByAuthorizedLocales(sales, authentication);
        
        // Ordenar por fecha más reciente primero
        return sales.stream()
                .sorted((a, b) -> b.getFechaVenta().compareTo(a.getFechaVenta()))
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene una venta por ID.
     */
    public Sale getSaleById(String id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id: " + id));
    }
    
    /**
     * Obtiene ventas por rango de fechas (filtradas por locales autorizados).
     */
    public List<Sale> getSalesByDateRange(LocalDateTime start, LocalDateTime end, org.springframework.security.core.Authentication authentication) {
        List<Sale> sales = saleRepository.findByFechaVentaBetween(start, end);
        return filterByAuthorizedLocales(sales, authentication);
    }
    

    /**
     * Obtiene ventas por vendedor.
     */
    public List<Sale> getSalesByVendedor(String vendedor) {
        return saleRepository.findByVendedor(vendedor);
    }
    
    /**
     * Obtiene estadísticas de ventas por mes (filtradas por locales autorizados y opcionalmente por un local específico).
     */
    public Map<String, Object> getSalesStatsByMonth(int year, int month, String localFilter, org.springframework.security.core.Authentication authentication) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        
        // Usar un rango amplio para incluir encargos con pagos en este mes
        LocalDateTime wideStart = start.minusMonths(12);
        List<Sale> sales = getSalesByDateRange(wideStart, end, authentication);
        
        // Aplicar filtro de local si se especificó
        if (localFilter != null && !localFilter.isEmpty()) {
            // Validate localId exists
            try {
                localService.validateActiveLocalId(localFilter);
                final String localId = localFilter;
                sales = sales.stream()
                        .filter(sale -> localId.equals(sale.getLocalId()))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Si el local no es válido, devolver lista vacía
                sales = List.of();
            }
        }
        
        // Calcular total de ventas sumando pagos que caen dentro del período
        double totalVentas = 0.0;
        Set<String> saleIdsIncluded = new HashSet<>();
        for (Sale sale : sales) {
            if (sale.getPayments() != null && !sale.getPayments().isEmpty()) {
                for (Payment payment : sale.getPayments()) {
                    if (payment != null && payment.getAmount() != null) {
                        LocalDateTime pymtDate = payment.getPaymentDate() != null ? payment.getPaymentDate() : sale.getFechaVenta();
                        if (pymtDate != null && !pymtDate.isBefore(start) && !pymtDate.isAfter(end)) {
                            totalVentas += payment.getAmount();
                            saleIdsIncluded.add(sale.getId());
                        }
                    }
                }
            } else {
                totalVentas += sale.getTotalCLP();
                saleIdsIncluded.add(sale.getId());
            }
        }
        
        int cantidadVentas = saleIdsIncluded.size();
        
        // Total por método de pago (suma de todos los pagos de cada tipo en el período)
        Map<String, Double> totalPorMetodo = new HashMap<>();
        for (Sale sale : sales) {
            if (sale.getPayments() != null && !sale.getPayments().isEmpty()) {
                for (Payment payment : sale.getPayments()) {
                    if (payment != null && payment.getPaymentMethod() != null && payment.getAmount() != null) {
                        LocalDateTime pymtDate = payment.getPaymentDate() != null ? payment.getPaymentDate() : sale.getFechaVenta();
                        if (pymtDate != null && !pymtDate.isBefore(start) && !pymtDate.isAfter(end)) {
                            totalPorMetodo.merge(payment.getPaymentMethod(), payment.getAmount(), Double::sum);
                        }
                    }
                }
            }
        }
        
        // Contar cantidad de ventas por método de pago
        Map<String, Integer> ventasPorMetodo = new HashMap<>();
        for (Sale sale : sales) {
            if (sale.getPayments() != null && !sale.getPayments().isEmpty()) {
                for (Payment payment : sale.getPayments()) {
                    if (payment != null && payment.getPaymentMethod() != null && payment.getAmount() != null) {
                        LocalDateTime pymtDate = payment.getPaymentDate() != null ? payment.getPaymentDate() : sale.getFechaVenta();
                        if (pymtDate != null && !pymtDate.isBefore(start) && !pymtDate.isAfter(end)) {
                            ventasPorMetodo.merge(payment.getPaymentMethod(), 1, Integer::sum);
                        }
                    }
                }
            }
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("periodo", String.format("%d-%02d", year, month));
        stats.put("totalVentas", totalVentas);
        stats.put("cantidadVentas", cantidadVentas);
        stats.put("promedioVenta", cantidadVentas > 0 ? totalVentas / cantidadVentas : 0);
        stats.put("totalPorMetodo", totalPorMetodo);
        stats.put("ventasPorMetodo", ventasPorMetodo);
        stats.put("ventas", sales);
        
        return stats;
    }
    
    /**
     * Obtiene estadísticas generales por rango de fechas (filtradas por locales autorizados y opcionalmente por un local específico).
     */
    public Map<String, Object> getSalesStatsByRange(LocalDateTime start, LocalDateTime end, String localFilter, org.springframework.security.core.Authentication authentication) {
        // Usar un rango amplio para incluir encargos con pagos en este período
        LocalDateTime wideStart = start != null ? start.minusMonths(12) : LocalDateTime.now().minusMonths(12);
        List<Sale> sales = getSalesByDateRange(wideStart, end, authentication);
        
        // Aplicar filtro de local si se especificó
        if (localFilter != null && !localFilter.isEmpty()) {
            // Validate localId exists
            try {
                localService.validateActiveLocalId(localFilter);
                final String localId = localFilter;
                sales = sales.stream()
                        .filter(sale -> localId.equals(sale.getLocalId()))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Si el local no es válido, devolver lista vacía
                sales = List.of();
            }
        }
        
        // Calcular total de ventas sumando pagos que caen dentro del rango
        double totalVentas = 0.0;
        Set<String> saleIdsIncluded = new HashSet<>();
        for (Sale sale : sales) {
            if (sale.getPayments() != null && !sale.getPayments().isEmpty()) {
                for (Payment payment : sale.getPayments()) {
                    if (payment != null && payment.getAmount() != null) {
                        LocalDateTime pymtDate = payment.getPaymentDate() != null ? payment.getPaymentDate() : sale.getFechaVenta();
                        if (pymtDate != null && !pymtDate.isBefore(start) && !pymtDate.isAfter(end)) {
                            totalVentas += payment.getAmount();
                            saleIdsIncluded.add(sale.getId());
                        }
                    }
                }
            } else {
                totalVentas += sale.getTotalCLP();
                saleIdsIncluded.add(sale.getId());
            }
        }
        
        int cantidadVentas = saleIdsIncluded.size();
        
        // Total por método de pago (suma de todos los pagos de cada tipo en el rango)
        Map<String, Double> totalPorMetodo = new HashMap<>();
        for (Sale sale : sales) {
            if (sale.getPayments() != null && !sale.getPayments().isEmpty()) {
                for (Payment payment : sale.getPayments()) {
                    if (payment != null && payment.getPaymentMethod() != null && payment.getAmount() != null) {
                        LocalDateTime pymtDate = payment.getPaymentDate() != null ? payment.getPaymentDate() : sale.getFechaVenta();
                        if (pymtDate != null && !pymtDate.isBefore(start) && !pymtDate.isAfter(end)) {
                            totalPorMetodo.merge(payment.getPaymentMethod(), payment.getAmount(), Double::sum);
                        }
                    }
                }
            }
        }
        
        // Contar cantidad de ventas por método de pago
        Map<String, Integer> ventasPorMetodo = new HashMap<>();
        for (Sale sale : sales) {
            if (sale.getPayments() != null && !sale.getPayments().isEmpty()) {
                for (Payment payment : sale.getPayments()) {
                    if (payment != null && payment.getPaymentMethod() != null && payment.getAmount() != null) {
                        LocalDateTime pymtDate = payment.getPaymentDate() != null ? payment.getPaymentDate() : sale.getFechaVenta();
                        if (pymtDate != null && !pymtDate.isBefore(start) && !pymtDate.isAfter(end)) {
                            ventasPorMetodo.merge(payment.getPaymentMethod(), 1, Integer::sum);
                        }
                    }
                }
            }
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("fechaInicio", start);
        stats.put("fechaFin", end);
        stats.put("totalVentas", totalVentas);
        stats.put("cantidadVentas", cantidadVentas);
        stats.put("promedioVenta", cantidadVentas > 0 ? totalVentas / cantidadVentas : 0);
        stats.put("totalPorMetodo", totalPorMetodo);
        stats.put("ventasPorMetodo", ventasPorMetodo);
        
        return stats;
    }
    
    /**
     * Obtiene despachos pendientes (ventas con tipo DESPACHO y estado POR_ENTREGAR).
     */
    public List<Sale> getPendingDeliveries() {
        return saleRepository.findByTipoEntregaAndEstadoEntrega("DESPACHO", "POR_ENTREGAR");
    }
    
    /**
     * Obtiene despachos pendientes para una fecha específica.
     */
    public List<Sale> getDeliveriesByDate(LocalDateTime date) {
        LocalDateTime start = date.toLocalDate().atStartOfDay();
        LocalDateTime end = date.toLocalDate().atTime(23, 59, 59);
        return saleRepository.findByEstadoEntregaAndFechaDespachoBetween("POR_ENTREGAR", start, end);
    }
    
    /**
     * Obtiene historial de despachos completados (entregados).
     * Filtrado por rango de fechas, local específico y locales autorizados.
     * Si el usuario es FLETERO, solo ve sus propias entregas.
     */
    public List<Sale> getCompletedDeliveries(LocalDateTime start, LocalDateTime end, String localFilter, org.springframework.security.core.Authentication authentication) {
        // Si no se proporcionan fechas, usar mes actual
        LocalDateTime startDate = start != null ? start : LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime endDate = end != null ? end : LocalDateTime.now().toLocalDate().atTime(23, 59, 59);
        
        // Obtener todas las ventas con tipo DESPACHO y estado ENTREGADO
        List<Sale> completedDeliveries = saleRepository.findByTipoEntregaAndEstadoEntrega("DESPACHO", "ENTREGADO");
        
        // Filtrar por rango de fechas (usando fechaEntrega)
        completedDeliveries = completedDeliveries.stream()
                .filter(sale -> sale.getFechaEntrega() != null)
                .filter(sale -> !sale.getFechaEntrega().isBefore(startDate))
                .filter(sale -> !sale.getFechaEntrega().isAfter(endDate))
                .collect(Collectors.toList());
        
        // Si es FLETERO, solo mostrar sus propias entregas
        boolean isFletero = authentication != null && 
                           authentication.getAuthorities().stream()
                           .anyMatch(auth -> auth.getAuthority().equals("ROLE_FLETERO"));
        
        if (isFletero && authentication != null) {
            String username = authentication.getName();
            completedDeliveries = completedDeliveries.stream()
                    .filter(sale -> username.equals(sale.getEntregadoPor()))
                    .collect(Collectors.toList());
        } else {
            // Para ADMIN y ADMIN_LOCAL, aplicar filtrado por locales autorizados
            completedDeliveries = filterByAuthorizedLocales(completedDeliveries, authentication);
        }
        
        // Aplicar filtro de local específico si se especificó
        if (localFilter != null && !localFilter.isEmpty()) {
            // Validate localId exists
            try {
                localService.validateActiveLocalId(localFilter);
                final String localId = localFilter;
                completedDeliveries = completedDeliveries.stream()
                        .filter(sale -> localId.equals(sale.getLocalId()))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                completedDeliveries = List.of();
            }
        }
        
        // Ordenar por fecha de entrega (más reciente primero)
        completedDeliveries.sort((a, b) -> b.getFechaEntrega().compareTo(a.getFechaEntrega()));
        
        return completedDeliveries;
    }
    
    /**
     * Marca una venta como entregada y registra quién la entregó, junto con información del flete.
     */
    @Transactional
    public Sale markAsDelivered(String saleId, String entregadoPor, Double montoFlete, String metodoPagoFlete) {
        Sale sale = getSaleById(saleId);
        sale.setEstadoEntrega("ENTREGADO");
        sale.setEntregadoPor(entregadoPor);
        sale.setFechaEntrega(LocalDateTime.now());
        
        // Registrar información del flete si se proporciona
        if (montoFlete != null && montoFlete > 0) {
            sale.setMontoFlete(montoFlete);
        }
        if (metodoPagoFlete != null && !metodoPagoFlete.isEmpty()) {
            sale.setMetodoPagoFlete(metodoPagoFlete);
        }
        
        return saleRepository.save(sale);
    }
    
    /**
     * Actualiza la fecha de despacho de una venta.
     */
    @Transactional
    public Sale updateDeliveryDate(String saleId, LocalDateTime newDate) {
        Sale sale = getSaleById(saleId);
        
        if (!"DESPACHO".equals(sale.getTipoEntrega())) {
            throw new BadRequestException("Solo se puede actualizar la fecha de despacho para ventas con tipo DESPACHO");
        }
        
        sale.setFechaDespacho(newDate);
        return saleRepository.save(sale);
    }
    
    /**
     * Obtiene estadísticas personales de un vendedor (del día y del mes).
     */
    public Map<String, Object> getVendedorStats(String vendedor) {
        LocalDateTime now = LocalDateTime.now();
        
        // Ventas del día
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59);
        List<Sale> ventasDelDia = saleRepository.findByVendedorAndFechaVentaBetween(vendedor, startOfDay, endOfDay);
        
        double totalDia = ventasDelDia.stream()
                .mapToDouble(Sale::getTotalCLP)
                .sum();
        
        // Ventas del mes
        LocalDateTime startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime endOfMonth = now.toLocalDate().atTime(23, 59, 59);
        List<Sale> ventasDelMes = saleRepository.findByVendedorAndFechaVentaBetween(vendedor, startOfMonth, endOfMonth);
        
        double totalMes = ventasDelMes.stream()
                .mapToDouble(Sale::getTotalCLP)
                .sum();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("vendedor", vendedor);
        stats.put("dia", Map.of(
            "cantidad", ventasDelDia.size(),
            "total", totalDia,
            "ventas", ventasDelDia
        ));
        stats.put("mes", Map.of(
            "cantidad", ventasDelMes.size(),
            "total", totalMes
        ));
        
        return stats;
    }
    
    /**
     * Obtiene estadísticas de todos los vendedores para el dashboard de admin (filtradas por locales y opcionalmente por un local específico).
     * Acepta rango de fechas opcional, por defecto usa el mes actual.
     */
    public List<Map<String, Object>> getStatsByAllSellers(String localFilter, LocalDateTime start, LocalDateTime end, org.springframework.security.core.Authentication authentication) {
        // Si no se proporcionan fechas, usar mes actual
        LocalDateTime startDate;
        LocalDateTime endDate;
        
        if (start != null && end != null) {
            startDate = start;
            endDate = end;
        } else {
            LocalDateTime now = LocalDateTime.now();
            startDate = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
            endDate = now.toLocalDate().atTime(23, 59, 59);
        }
        
        // Usar un rango amplio para incluir encargos con pagos en este período
        LocalDateTime wideStartDate = startDate.minusMonths(12);
        List<Sale> allSales = getSalesByDateRange(wideStartDate, endDate, authentication);
        
        // Definir variables finales para usar en lambdas
        final LocalDateTime periodStart = startDate;
        final LocalDateTime periodEnd = endDate;
        
        // Aplicar filtro de local si se especificó
        if (localFilter != null && !localFilter.isEmpty()) {
            try {
                localService.validateActiveLocalId(localFilter);
                final String localId = localFilter;
                allSales = allSales.stream()
                        .filter(sale -> localId.equals(sale.getLocalId()))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                allSales = List.of();
            }
        }
        
        // Agrupar por vendedor
        Map<String, List<Sale>> salesByVendor = allSales.stream()
                .filter(sale -> sale.getVendedor() != null && !sale.getVendedor().isEmpty())
                .collect(Collectors.groupingBy(Sale::getVendedor));
        
        return salesByVendor.entrySet().stream()
                .map(entry -> {
                    String vendedor = entry.getKey();
                    List<Sale> ventas = entry.getValue();
                    
                    // Calcular total sumando pagos del período
                    double total = 0.0;
                    int cantidad = 0;
                    for (Sale sale : ventas) {
                        boolean hasPaymentInPeriod = false;
                        if (sale.getPayments() != null && !sale.getPayments().isEmpty()) {
                            for (Payment payment : sale.getPayments()) {
                                if (payment != null && payment.getAmount() != null) {
                                    LocalDateTime pymtDate = payment.getPaymentDate() != null ? payment.getPaymentDate() : sale.getFechaVenta();
                                    if (pymtDate != null && !pymtDate.isBefore(periodStart) && !pymtDate.isAfter(periodEnd)) {
                                        total += payment.getAmount();
                                        hasPaymentInPeriod = true;
                                    }
                                }
                            }
                        }
                        if (!hasPaymentInPeriod && sale.getPayments() != null && !sale.getPayments().isEmpty() == false) {
                            total += sale.getTotalCLP();
                            cantidad++;
                        } else if (hasPaymentInPeriod) {
                            cantidad++;
                        }
                    }
                    
                    Map<String, Object> vendedorStats = new HashMap<>();
                    vendedorStats.put("vendedor", vendedor);
                    vendedorStats.put("cantidadVentas", cantidad);
                    vendedorStats.put("totalVentas", total);
                    
                    return vendedorStats;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Filtra las ventas por los locales autorizados del usuario.
     * - ADMINISTRADOR: Ve todas las ventas (sin filtro)
     * - ADMIN_LOCAL/VENDEDOR: Solo ve ventas de sus locales asignados
     */
    private List<Sale> filterByAuthorizedLocales(List<Sale> sales, org.springframework.security.core.Authentication authentication) {
        if (authentication == null) {
            return sales;
        }
        
        List<String> authorizedLocales = authHelper.getAuthorizedLocales(authentication);
        
        // ADMINISTRADOR ve todas
        if (authHelper.isGlobalAdmin(authentication)) {
            return sales;
        }
        
        // Filtrar por locales autorizados
        return sales.stream()
                .filter(sale -> authorizedLocales.contains(sale.getLocalId()))
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene estadísticas agrupadas por local (del mes actual, filtradas por locales autorizados y opcionalmente por un local específico).
     */
    public List<Map<String, Object>> getStatsByLocales(String localFilter, org.springframework.security.core.Authentication authentication) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime endOfMonth = now.toLocalDate().atTime(23, 59, 59);
        
        // Usar un rango amplio para incluir encargos con pagos en este mes
        LocalDateTime wideStartOfMonth = startOfMonth.minusMonths(12);
        List<Sale> allSales = getSalesByDateRange(wideStartOfMonth, endOfMonth, authentication);
        
        // Filtrar ventas que no tienen local asignado (datos inconsistentes)
        allSales = allSales.stream()
                .filter(sale -> sale.getLocalId() != null)
                .collect(Collectors.toList());
        
        final LocalDateTime periodStart = startOfMonth;
        final LocalDateTime periodEnd = endOfMonth;
        
        // Aplicar filtro de local si se especificó
        if (localFilter != null && !localFilter.isEmpty()) {
            try {
                localService.validateActiveLocalId(localFilter);
                final String localId = localFilter;
                allSales = allSales.stream()
                        .filter(sale -> localId.equals(sale.getLocalId()))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                allSales = List.of();
            }
        }
        
        // Agrupar por local
        Map<String, List<Sale>> salesByLocal = allSales.stream()
                .collect(Collectors.groupingBy(Sale::getLocalId));
        
        return salesByLocal.entrySet().stream()
                .map(entry -> {
                    String localId = entry.getKey();
                    List<Sale> ventas = entry.getValue();
                    
                    // Calcular total sumando pagos del mes
                    double totalVentas = 0.0;
                    int cantidadVentas = 0;
                    for (Sale sale : ventas) {
                        boolean hasPaymentInPeriod = false;
                        if (sale.getPayments() != null && !sale.getPayments().isEmpty()) {
                            for (Payment payment : sale.getPayments()) {
                                if (payment != null && payment.getAmount() != null) {
                                    LocalDateTime pymtDate = payment.getPaymentDate() != null ? payment.getPaymentDate() : sale.getFechaVenta();
                                    if (pymtDate != null && !pymtDate.isBefore(periodStart) && !pymtDate.isAfter(periodEnd)) {
                                        totalVentas += payment.getAmount();
                                        hasPaymentInPeriod = true;
                                    }
                                }
                            }
                        }
                        if (!hasPaymentInPeriod && (sale.getPayments() == null || sale.getPayments().isEmpty())) {
                            totalVentas += sale.getTotalCLP();
                            cantidadVentas++;
                        } else if (hasPaymentInPeriod) {
                            cantidadVentas++;
                        }
                    }
                    
                    // Ventas por canal
                    Map<String, Long> ventasPorCanal = ventas.stream()
                            .filter(sale -> sale.getCanalVenta() != null)
                            .collect(Collectors.groupingBy(
                                sale -> sale.getCanalVenta().name(),
                                Collectors.counting()
                            ));
                    
                    Map<String, Object> localStats = new HashMap<>();
                    localStats.put("local", localId);
                    localStats.put("cantidadVentas", cantidadVentas);
                    localStats.put("totalVentas", totalVentas);
                    localStats.put("ventasPorCanal", ventasPorCanal);
                    
                    return localStats;
                })
                .sorted((a, b) -> Double.compare(
                    (Double) b.get("totalVentas"),
                    (Double) a.get("totalVentas")
                ))
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene estadísticas de fletes (despachos entregados con montoFlete registrado).
     * Filtrado por rango de fechas, local específico y locales autorizados.
     */
    public Map<String, Object> getFreightStats(LocalDateTime start, LocalDateTime end, String localFilter, org.springframework.security.core.Authentication authentication) {
        // Si no se proporcionan fechas, usar mes actual
        LocalDateTime startDate = start != null ? start : LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime endDate = end != null ? end : LocalDateTime.now().toLocalDate().atTime(23, 59, 59);
        
        // Obtener ventas del rango de fechas, filtradas por locales autorizados
        List<Sale> allSales = getSalesByDateRange(startDate, endDate, authentication);
        
        // Filtrar solo despachos entregados con flete registrado
        List<Sale> fletes = allSales.stream()
                .filter(sale -> "DESPACHO".equals(sale.getTipoEntrega()))
                .filter(sale -> "ENTREGADO".equals(sale.getEstadoEntrega()))
                .filter(sale -> sale.getMontoFlete() != null && sale.getMontoFlete() > 0)
                .filter(sale -> sale.getLocalId() != null) // Filtrar ventas sin local
                .collect(Collectors.toList());
        
        // Aplicar filtro de local si se especificó
        if (localFilter != null && !localFilter.isEmpty()) {
            // Validate localId exists
            try {
                localService.validateActiveLocalId(localFilter);
                final String localId = localFilter;
                fletes = fletes.stream()
                        .filter(sale -> localId.equals(sale.getLocalId()))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                fletes = List.of();
            }
        }
        
        double totalFletes = fletes.stream()
                .mapToDouble(Sale::getMontoFlete)
                .sum();
        
        int cantidadFletes = fletes.size();
        
        // Fletes por método de pago
        Map<String, Long> fletesPorMetodo = fletes.stream()
                .filter(sale -> sale.getMetodoPagoFlete() != null)
                .collect(Collectors.groupingBy(Sale::getMetodoPagoFlete, Collectors.counting()));
        
        // Total por método de pago
        Map<String, Double> totalPorMetodo = fletes.stream()
                .filter(sale -> sale.getMetodoPagoFlete() != null)
                .collect(Collectors.groupingBy(
                    Sale::getMetodoPagoFlete,
                    Collectors.summingDouble(Sale::getMontoFlete)
                ));
        
        // Fletes por local (ahora seguro que todos tienen localId != null)
        Map<String, Long> fletesPorLocal = fletes.stream()
                .collect(Collectors.groupingBy(
                    Sale::getLocalId,
                    Collectors.counting()
                ));
        
        // Total por local
        Map<String, Double> totalPorLocal = fletes.stream()
                .collect(Collectors.groupingBy(
                    Sale::getLocalId,
                    Collectors.summingDouble(Sale::getMontoFlete)
                ));
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFletes", totalFletes);
        stats.put("cantidadFletes", cantidadFletes);
        stats.put("promedioFlete", cantidadFletes > 0 ? totalFletes / cantidadFletes : 0);
        stats.put("fletesPorMetodo", fletesPorMetodo);
        stats.put("totalPorMetodo", totalPorMetodo);
        stats.put("fletesPorLocal", fletesPorLocal);
        stats.put("totalPorLocal", totalPorLocal);
        
        return stats;
    }
    
    /**
     * Obtiene ventas pendientes de aprobación filtradas por locales del usuario.
     */
    public List<Sale> getPendingSales(org.springframework.security.core.Authentication authentication) {
        List<Sale> pendingSales = saleRepository.findByEstadoAprobacion("PENDIENTE_APROBACION");
        
        // Filtrar por locales del usuario autenticado
        List<String> userLocales = authHelper.getUserLocales(authentication);
        if (authHelper.isAdmin(authentication)) {
            return pendingSales; // Admin ve todas
        }
        
        return pendingSales.stream()
                .filter(sale -> userLocales.contains(sale.getLocalId()))
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene ventas aprobadas filtradas por locales del usuario.
     * - ADMINISTRADOR: Ve todas las ventas aprobadas
     * - ADMIN_LOCAL, ENCARGADO_LOCAL: Ve ventas aprobadas de sus locales
     * - VENDEDOR, VENDEDOR_SIN_COMISION: Ve solo sus propias ventas aprobadas (donde fueron asignados como vendedor)
     */
    public List<Sale> getApprovedSales(org.springframework.security.core.Authentication authentication) {
        List<Sale> approvedSales = saleRepository.findByEstadoAprobacion("APROBADA");

        // Obtener el usuario autenticado
        User user = authHelper.getAuthenticatedUser(authentication);
        if (user == null) {
            return List.of();
        }

        // ADMINISTRADOR ve todas las ventas aprobadas
        if (user.getRole() == Role.ADMINISTRADOR) {
            return approvedSales;
        }

        // VENDEDOR y VENDEDOR_SIN_COMISION: solo ven sus propias ventas
        if (user.getRole() == Role.VENDEDOR || user.getRole() == Role.VENDEDOR_SIN_COMISION) {
            String username = user.getUsername();
            return approvedSales.stream()
                    .filter(sale -> username.equals(sale.getVendedor()))
                    .collect(Collectors.toList());
        }

        // ADMIN_LOCAL, ENCARGADO_LOCAL: ven ventas de sus locales asignados
        List<String> userLocales = authHelper.getUserLocales(authentication);
        return approvedSales.stream()
                .filter(sale -> userLocales.contains(sale.getLocalId()))
                .collect(Collectors.toList());
    }
    
    /**
     * Aprueba una venta pendiente.
     */
    @Transactional
    public Sale approveSale(String saleId, String approvedBy) {
        Sale sale = getSaleById(saleId);
        
        if (!"PENDIENTE_APROBACION".equals(sale.getEstadoAprobacion())) {
            throw new BadRequestException("La venta no está pendiente de aprobación");
        }
        
        sale.setEstadoAprobacion("APROBADA");
        sale.setAprobadoPor(approvedBy);
        sale.setFechaAprobacion(LocalDateTime.now());
        
        Sale approvedSale = saleRepository.save(sale);
        
        // Descontar stock ahora que la venta fue aprobada (confirma reserva + descuenta stock)
        for (SaleItem item : approvedSale.getItems()) {
            productService.confirmarReserva(item.getProductId(), approvedSale.getLocalId(), item.getCantidad());
        }
        
        // Crear comisión ahora que la venta fue aprobada
        try {
            commissionService.createCommission(approvedSale, sale.getVendedor());
        } catch (Exception e) {
            System.err.println("Error creando comisión: " + e.getMessage());
        }
        
        // Crear comisiones para ADMIN_LOCAL que tienen el local en su lista de comisiones
        try {
            createAdminLocalCommissions(approvedSale);
        } catch (Exception e) {
            System.err.println("Error creando comisiones ADMIN_LOCAL: " + e.getMessage());
        }
        
        return approvedSale;
    }
    
    /**
     * Rechaza una venta pendiente.
     * No se devuelve stock porque nunca fue descontado (se descuenta solo al aprobar).
     */
    @Transactional
    public Sale rejectSale(String saleId, String rejectedBy, String motivo) {
        Sale sale = getSaleById(saleId);
        
        if (!"PENDIENTE_APROBACION".equals(sale.getEstadoAprobacion())) {
            throw new BadRequestException("La venta no está pendiente de aprobación");
        }
        
        sale.setEstadoAprobacion("RECHAZADA");
        sale.setAprobadoPor(rejectedBy);
        sale.setFechaAprobacion(LocalDateTime.now());
        sale.setMotivoRechazo(motivo);

        return saleRepository.save(sale);
    }

    /**
     * Registra un abono en un encargo.
     * Solo ADMINISTRADOR, ADMIN_LOCAL y ENCARGADO_LOCAL pueden registrar abonos.
     */
    @Transactional
    public Sale abonarEncargo(String saleId, AbonoRequest abonoRequest, String registradoPor) {
        Sale sale = getSaleById(saleId);

        // Validar que sea un encargo
        if (!"ENCARGO".equals(sale.getTipoVenta())) {
            throw new BadRequestException("Esta venta no es un encargo");
        }

        // Validar que no esté cancelado
        if ("CANCELADO".equals(sale.getEstadoPago())) {
            throw new BadRequestException("No se puede abonar a un encargo cancelado");
        }

        // Validar que no esté pagado completo
        if ("PAGADO_COMPLETO".equals(sale.getEstadoPago())) {
            throw new BadRequestException("El encargo ya está pagado completamente");
        }

        // Validar el abono
        if (abonoRequest.getAmount() == null || abonoRequest.getAmount() <= 0) {
            throw new BadRequestException("El monto del abono debe ser mayor a 0");
        }

        Set<String> metodosValidos = Set.of("EFECTIVO", "TRANSFERENCIA", "DEBITO", "CREDITO");
        if (!metodosValidos.contains(abonoRequest.getPaymentMethod())) {
            throw new BadRequestException("Forma de pago inválida: " + abonoRequest.getPaymentMethod());
        }

        // Calcular nuevo abono
        double nuevoAbono = abonoRequest.getAmount();
        double totalAbonadoActual = sale.getTotalAbonado() != null ? sale.getTotalAbonado() : 0.0;
        double nuevoTotalAbonado = totalAbonadoActual + nuevoAbono;

        // Validar que no exceda el total
        if (nuevoTotalAbonado > sale.getTotalCLP()) {
            throw new BadRequestException(
                String.format("El abono excede el saldo pendiente. Saldo pendiente: $%,.0f", 
                    sale.getTotalCLP() - totalAbonadoActual)
            );
        }

        // Crear nuevo pago
        Payment newPayment = Payment.builder()
                .paymentMethod(abonoRequest.getPaymentMethod())
                .amount(nuevoAbono)
                .paymentDate(LocalDateTime.now())
                .build();

        // Agregar a la lista de pagos
        List<Payment> payments = sale.getPayments();
        if (payments == null) {
            payments = new ArrayList<>();
        }
        payments.add(newPayment);
        sale.setPayments(payments);

        // Actualizar total abonado y saldo pendiente
        sale.setTotalAbonado(nuevoTotalAbonado);
        sale.setSaldoPendiente(sale.getTotalCLP() - nuevoTotalAbonado);

        // Actualizar estado de pago
        if (nuevoTotalAbonado >= sale.getTotalCLP()) {
            sale.setEstadoPago("PAGADO_COMPLETO");
            // Descontar stock cuando se paga completo (confirma reserva + descuenta stock)
            for (SaleItem item : sale.getItems()) {
                productService.confirmarReserva(item.getProductId(), sale.getLocalId(), item.getCantidad());
            }
        } else if (nuevoTotalAbonado > 0) {
            sale.setEstadoPago("ABONADO_PARCIAL");
        }

        return saleRepository.save(sale);
    }

    /**
     * Cancela un encargo y elimina la comisión generada.
     * Solo ADMINISTRADOR, ADMIN_LOCAL y ENCARGADO_LOCAL pueden cancelar.
     * Registra la devolución en las notas.
     */
    @Transactional
    public Sale cancelarEncargo(String saleId, String canceladoPor, String motivo) {
        Sale sale = getSaleById(saleId);

        // Validar que sea un encargo
        if (!"ENCARGO".equals(sale.getTipoVenta())) {
            throw new BadRequestException("Esta venta no es un encargo");
        }

        // Validar que no esté ya cancelado
        if ("CANCELADO".equals(sale.getEstadoPago())) {
            throw new BadRequestException("El encargo ya está cancelado");
        }

        // Devolver stock si el encargo estaba PAGADO_COMPLETO (stock ya fue descontado)
        if ("PAGADO_COMPLETO".equals(sale.getEstadoPago())) {
            for (SaleItem item : sale.getItems()) {
                productService.increaseStock(item.getProductId(), sale.getLocalId(), item.getCantidad());
            }
        }

        // Cambiar estado a cancelado
        sale.setEstadoPago("CANCELADO");

        // Eliminar comisión si existe
        try {
            commissionService.deleteCommissionBySaleId(saleId);
        } catch (Exception e) {
            System.err.println("Error eliminando comisión: " + e.getMessage());
        }

        // Registrar en notas la devolución
        String notaCancelacion = String.format(
            "[CANCELADO] Fecha: %s | Por: %s | Motivo: %s | Total abonado: $%,.0f",
            LocalDateTime.now(), canceladoPor, motivo, sale.getTotalAbonado() != null ? sale.getTotalAbonado() : 0.0
        );
        String notasActuales = sale.getNotas() != null ? sale.getNotas() : "";
        sale.setNotas(notasActuales + "\n" + notaCancelacion);

        return saleRepository.save(sale);
    }

    /**
     * Elimina una venta del sistema. Solo permitido para ADMINISTRADOR, ADMIN_LOCAL y ENCARGADO_LOCAL.
     * Si la venta fue aprobada, devuelve el stock de los productos al local.
     */
    @Transactional
    public void deleteSale(String saleId, org.springframework.security.core.Authentication authentication) {
        Sale sale = getSaleById(saleId);

        // Validar Roles: Administrador, Admin Local o Encargado Local
        User user = authHelper.getAuthenticatedUser(authentication);
        if (user == null || !(user.getRole() == Role.ADMINISTRADOR ||
                               user.getRole() == Role.ADMIN_LOCAL ||
                               user.getRole() == Role.ENCARGADO_LOCAL)) {
            throw new BadRequestException("No tiene permisos suficientes para eliminar esta venta");
        }

        // Si la venta estaba APROBADA, devolver el stock al local
        if ("APROBADA".equals(sale.getEstadoAprobacion())) {
            for (SaleItem item : sale.getItems()) {
                productService.increaseStock(item.getProductId(), sale.getLocalId(), item.getCantidad());
            }
            // Eliminar comisión asociada si existe
            commissionService.deleteCommissionBySaleId(saleId);
        }

        saleRepository.deleteById(saleId);
    }

    /**
     * Actualiza una venta existente (solo ciertos campos).
     * Solo se pueden actualizar: items, metodoPago, información del cliente y fecha de despacho.
     * IMPORTANTE: Solo se permite actualizar ventas que NO hayan sido entregadas.
     */
    @Transactional
    public Sale updateSale(String saleId, com.muebleria.dto.SaleUpdateRequest request, org.springframework.security.core.Authentication authentication) {
        Sale sale = getSaleById(saleId);
        
        // Validar que la venta no haya sido entregada
        if ("ENTREGADO".equals(sale.getEstadoEntrega())) {
            throw new BadRequestException("No se puede editar una venta que ya fue entregada");
        }
        
        // Validar que la venta no esté rechazada
        if ("RECHAZADA".equals(sale.getEstadoAprobacion())) {
            throw new BadRequestException("No se puede editar una venta rechazada");
        }
        
        String saleLocal = sale.getLocalId();
        
        // Si hay cambios en los items, necesitamos ajustar el stock
        boolean itemsChanged = !itemsEqual(sale.getItems(), request.getItems());
        
        if (itemsChanged) {
            // Devolver el stock de los items antiguos (solo si la venta ya fue aprobada)
            if ("APROBADA".equals(sale.getEstadoAprobacion())) {
                for (SaleItem oldItem : sale.getItems()) {
                    productService.increaseStock(oldItem.getProductId(), saleLocal, oldItem.getCantidad());
                }
            }
            
            // Construir nuevos items y calcular total
            List<SaleItem> newItems = new ArrayList<>();
            Double newTotal = 0.0;
            
            for (com.muebleria.dto.SaleItemRequest itemReq : request.getItems()) {
                Product product = productService.getProductById(itemReq.getProductId());
                
                // Validar stock solo si la venta ya fue aprobada
                if ("APROBADA".equals(sale.getEstadoAprobacion())) {
                    if (!productService.hasStock(product.getId(), saleLocal, itemReq.getCantidad())) {
                        throw new BadRequestException(
                            "Stock insuficiente para " + product.getNombre() + 
                            " en local " + saleLocal + ". Stock disponible: " + 
                            productService.getProductById(product.getId()).getStock()
                        );
                    }
                }
                
                double precioConDescuento = product.getPrecio() - (itemReq.getDescuento() != null ? itemReq.getDescuento() : 0);
                SaleItem saleItem = SaleItem.builder()
                        .productId(product.getId())
                        .productName(product.getNombre())
                        .cantidad(itemReq.getCantidad())
                        .precioUnitario(product.getPrecio())
                        .descuento(itemReq.getDescuento())
                        .subtotal(precioConDescuento * itemReq.getCantidad())
                        .build();
                
                newItems.add(saleItem);
                newTotal += saleItem.getSubtotal();
            }
            
            // Descontar el nuevo stock (solo si la venta ya fue aprobada)
            if ("APROBADA".equals(sale.getEstadoAprobacion())) {
                for (SaleItem newItem : newItems) {
                    productService.confirmarReserva(newItem.getProductId(), saleLocal, newItem.getCantidad());
                }
                
                // Actualizar o recalcular comisión si es necesario
                // Por simplicidad, eliminar la comisión antigua y crear una nueva
                commissionService.deleteCommissionBySaleId(saleId);
                
                // Crear nueva comisión con la venta actualizada
                sale.setItems(newItems);
                sale.setTotalCLP(newTotal);
                commissionService.createCommission(sale, sale.getVendedor());
                createAdminLocalCommissions(sale);
            }
            
            sale.setItems(newItems);
            sale.setTotalCLP(newTotal);
        }
        
        // Actualizar métodos de pago
        if (request.getPayments() != null && !request.getPayments().isEmpty()) {
        validatePayments(request.getPayments());
            // Encargos permiten pagos parciales; ventas normales requieren igualdad
            if (!"ENCARGO".equals(sale.getTipoVenta())) {
                validatePaymentTotal(request.getPayments(), sale.getTotalCLP());
            }
            
            List<Payment> pagos = request.getPayments().stream()
                    .map(pr -> Payment.builder()
                            .paymentMethod(pr.getPaymentMethod())
                            .amount(pr.getAmount())
                            .build())
                    .collect(Collectors.toList());
            sale.setPayments(pagos);
        }
        
        // Actualizar otros campos
        sale.setClienteNombre(request.getClienteNombre());
        sale.setClienteDireccion(request.getClienteDireccion());
        sale.setClienteCorreo(request.getClienteCorreo());
        sale.setClienteTelefono(request.getClienteTelefono());
        sale.setFechaDespacho(request.getFechaDespacho());
        sale.setNotas(request.getNotas());
        
        return saleRepository.save(sale);
    }
    
    /**
     * Compara si dos listas de items son iguales (mismo producto y cantidad)
     */
    private boolean itemsEqual(List<SaleItem> items1, List<com.muebleria.dto.SaleItemRequest> items2) {
        if (items1.size() != items2.size()) {
            return false;
        }
        
        Map<String, Integer> map1 = items1.stream()
            .collect(Collectors.toMap(SaleItem::getProductId, SaleItem::getCantidad));
        
        for (com.muebleria.dto.SaleItemRequest item : items2) {
            if (!map1.containsKey(item.getProductId()) || 
                !map1.get(item.getProductId()).equals(item.getCantidad())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Obtiene lista de vendedores únicos (filtrados por locales autorizados)
     */
    public List<String> getUniqueVendedores(org.springframework.security.core.Authentication authentication) {
        List<Sale> allSales = getAllSales(authentication);
        return allSales.stream()
                .map(Sale::getVendedor)
                .filter(vendedor -> vendedor != null && !vendedor.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Obtiene todos los encargos (filtrados por locales autorizados)
     * Excluye encargos cancelados para evitar que aparezcan en estadísticas
     */
    public List<Sale> getAllEncargos(org.springframework.security.core.Authentication authentication) {
        List<Sale> encargos = saleRepository.findByTipoVenta("ENCARGO");
        encargos = encargos.stream()
            .filter(e -> !"CANCELADO".equals(e.getEstadoPago()))
            .collect(Collectors.toList());
        return filterByAuthorizedLocales(encargos, authentication);
    }

    /**
     * Obtiene encargos filtrados por estado de pago (filtrados por locales autorizados)
     */
    public List<Sale> getEncargosByEstadoPago(String estadoPago, org.springframework.security.core.Authentication authentication) {
        List<Sale> encargos = saleRepository.findByTipoVentaAndEstadoPago("ENCARGO", estadoPago);
        return filterByAuthorizedLocales(encargos, authentication);
    }

    /**
     * Obtiene encargos con filtros opcionales (local, estado pago, nombre cliente)
     * Excluye encargos cancelados
     */
    public List<Sale> getEncargosFiltered(String estadoPago, String localId, String clienteNombre, org.springframework.security.core.Authentication authentication) {
        List<Sale> encargos;

        if (estadoPago != null && localId != null && clienteNombre != null && !clienteNombre.isEmpty()) {
            encargos = saleRepository.findByTipoVentaAndEstadoPagoAndLocalIdAndClienteNombreContainingIgnoreCase("ENCARGO", estadoPago, localId, clienteNombre);
        } else if (estadoPago != null && localId != null) {
            encargos = saleRepository.findByTipoVentaAndEstadoPagoAndLocalId("ENCARGO", estadoPago, localId);
        } else if (estadoPago != null && clienteNombre != null && !clienteNombre.isEmpty()) {
            encargos = saleRepository.findByTipoVentaAndEstadoPagoAndClienteNombreContainingIgnoreCase("ENCARGO", estadoPago, clienteNombre);
        } else if (localId != null && clienteNombre != null && !clienteNombre.isEmpty()) {
            encargos = saleRepository.findByTipoVentaAndLocalIdAndClienteNombreContainingIgnoreCase("ENCARGO", localId, clienteNombre);
        } else if (estadoPago != null) {
            encargos = saleRepository.findByTipoVentaAndEstadoPago("ENCARGO", estadoPago);
        } else if (localId != null) {
            encargos = saleRepository.findByTipoVentaAndLocalId("ENCARGO", localId);
        } else if (clienteNombre != null && !clienteNombre.isEmpty()) {
            encargos = saleRepository.findByTipoVentaAndClienteNombreContainingIgnoreCase("ENCARGO", clienteNombre);
        } else {
            encargos = saleRepository.findByTipoVenta("ENCARGO");
        }

        encargos = encargos.stream()
            .filter(e -> !"CANCELADO".equals(e.getEstadoPago()))
            .collect(Collectors.toList());

        return filterByAuthorizedLocales(encargos, authentication);
    }
    
    /**
     * Obtiene estadísticas de ventas para todos los meses de un año
     */
    public Map<String, Map<String, Object>> getYearStats(int year, String localFilter, org.springframework.security.core.Authentication authentication) {
        Map<String, Map<String, Object>> yearStats = new java.util.LinkedHashMap<>();
        
        for (int month = 1; month <= 12; month++) {
            try {
                Map<String, Object> monthStats = getSalesStatsByMonth(year, month, localFilter, authentication);
                yearStats.put(String.valueOf(month), monthStats);
            } catch (Exception e) {
                // Si hay error en un mes, continuar con los demás
                Map<String, Object> emptyStats = new java.util.HashMap<>();
                emptyStats.put("totalVentas", 0.0);
                emptyStats.put("cantidadVentas", 0);
                emptyStats.put("promedioVenta", 0.0);
                yearStats.put(String.valueOf(month), emptyStats);
            }
        }
        
        return yearStats;
    }

    /**
     * Valida que todos los pagos tengan una forma de pago válida.
     */
    private void validatePayments(List<PaymentRequest> payments) {
        if (payments == null || payments.isEmpty()) {
            throw new BadRequestException("Debe incluir al menos un método de pago");
        }
        
        Set<String> metodosValidos = Set.of("EFECTIVO", "TRANSFERENCIA", "DEBITO", "CREDITO");
        
        for (PaymentRequest payment : payments) {
            if (payment.getPaymentMethod() == null || payment.getPaymentMethod().isEmpty()) {
                throw new BadRequestException("La forma de pago no puede estar vacía");
            }
            
            if (!metodosValidos.contains(payment.getPaymentMethod())) {
                throw new BadRequestException("Forma de pago inválida: " + payment.getPaymentMethod() + 
                    ". Debe ser uno de: EFECTIVO, TRANSFERENCIA, DEBITO, CREDITO");
            }
            
            if (payment.getAmount() == null || payment.getAmount() <= 0) {
                throw new BadRequestException("El monto de cada pago debe ser mayor a 0");
            }
        }
    }
    
    /**
     * Valida que la suma de los pagos sea igual al total de la venta.
     */
    private void validatePaymentTotal(List<PaymentRequest> payments, double total) {
        double sumaPagos = payments.stream()
                .mapToDouble(PaymentRequest::getAmount)
                .sum();
        
        // Usar una tolerancia para comparar doubles (0.01 CLP)
        double tolerance = 0.01;
        if (Math.abs(sumaPagos - total) > tolerance) {
            throw new BadRequestException(
                String.format("La suma de los pagos ($%,.0f) debe ser igual al total de la venta ($%,.0f)", 
                    sumaPagos, total)
            );
        }
    }
}
