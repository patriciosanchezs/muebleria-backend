package com.muebleria.service;

import com.muebleria.dto.PagoRequest;
import com.muebleria.dto.SaleItemRequest;
import com.muebleria.dto.SaleRequest;
import com.muebleria.exception.BadRequestException;
import com.muebleria.exception.ResourceNotFoundException;
import com.muebleria.model.CanalVenta;
import com.muebleria.model.Pago;
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
        
        // 2. Validar que todos los productos tengan stock suficiente en el local especificado
        validateStockByLocal(request.getItems(), localId);
        
        // 3. Validar pagos
        validatePagos(request.getPagos());
        
        // Determinar el dueño de la venta: vendedor asignado o usuario de sesión
        String vendedorFinal = request.getVendedorAsignado() != null && !request.getVendedorAsignado().isEmpty() 
                ? request.getVendedorAsignado() 
                : vendedor;
        
        // Obtener usuario actual (quien registra la venta) para registrar quien aplicó descuentos
        User currentUser = userRepository.findByUsername(vendedor)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Validar que si es DESPACHO, el monto del flete esté presente (puede ser 0)
        if ("DESPACHO".equals(request.getTipoEntrega()) && request.getMontoFlete() == null) {
            throw new BadRequestException("El monto del flete es requerido para despachos a domicilio");
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

        // 4. Validar que la suma de pagos sea igual al total
        validatePaymentTotal(request.getPagos(), totalCLP);
        
        // 5. Convertir PagoRequest a Pago
        List<Pago> pagos = request.getPagos().stream()
                .map(pr -> Pago.builder()
                        .formaDePago(pr.getFormaDePago())
                        .monto(pr.getMonto())
                        .build())
                .collect(Collectors.toList());
        
        // 6. Crear la venta
        Sale.SaleBuilder saleBuilder = Sale.builder()
                .items(saleItems)
                .totalCLP(totalCLP)
                .pagos(pagos)  // Lista de pagos múltiples
                .vendedor(vendedorFinal)  // Vendedor asignado o usuario de sesión
                .clienteNombre(request.getClienteNombre())
                .clienteDireccion(request.getClienteDireccion())
                .clienteCorreo(request.getClienteCorreo())
                .clienteTelefono(request.getClienteTelefono())
                .tipoEntrega(request.getTipoEntrega())
                .localId(localId)
                .canalVenta(canalVenta)
                .notas(request.getNotas())
                .fechaVenta(LocalDateTime.now());
        
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
        
        // 5. Descontar stock solo si la venta está APROBADA
        // Para ventas pendientes, el stock se descuenta al aprobar
        if ("APROBADA".equals(savedSale.getEstadoAprobacion())) {
            for (SaleItemRequest itemRequest : request.getItems()) {
                productService.decreaseStock(itemRequest.getProductId(), localId, itemRequest.getCantidad());
            }
        }
        
        // 6. Crear comisión solo si la venta está APROBADA
        // Para vendedores y encargados locales, la comisión se crea al aprobar la venta
        if ("APROBADA".equals(savedSale.getEstadoAprobacion())) {
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
            commissionService.createAdminLocalCommission(sale, adminLocal.getId());
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
                    .filter(sale -> sale.getPagos() != null && sale.getPagos().stream()
                            .anyMatch(pago -> metodoPago.equals(pago.getFormaDePago())))
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
        
        List<Sale> sales = getSalesByDateRange(start, end, authentication);
        
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
        
        double totalVentas = sales.stream()
                .mapToDouble(Sale::getTotalCLP)
                .sum();
        
        int cantidadVentas = sales.size();
        
        // Total por método de pago (suma de todos los pagos de cada tipo)
        Map<String, Double> totalPorMetodo = new HashMap<>();
        for (Sale sale : sales) {
            if (sale.getPagos() != null && !sale.getPagos().isEmpty()) {
                for (Pago pago : sale.getPagos()) {
                    if (pago != null && pago.getFormaDePago() != null && pago.getMonto() != null) {
                        totalPorMetodo.merge(pago.getFormaDePago(), pago.getMonto(), Double::sum);
                    }
                }
            }
        }
        
        // Contar cantidad de ventas por método de pago
        Map<String, Integer> ventasPorMetodo = new HashMap<>();
        for (Sale sale : sales) {
            if (sale.getPagos() != null && !sale.getPagos().isEmpty()) {
                for (Pago pago : sale.getPagos()) {
                    if (pago != null && pago.getFormaDePago() != null && pago.getMonto() != null) {
                        ventasPorMetodo.merge(pago.getFormaDePago(), 1, Integer::sum);
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
        List<Sale> sales = getSalesByDateRange(start, end, authentication);
        
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
        
        double totalVentas = sales.stream()
                .mapToDouble(Sale::getTotalCLP)
                .sum();
        
        int cantidadVentas = sales.size();
        
        // Total por método de pago (suma de todos los pagos de cada tipo)
        Map<String, Double> totalPorMetodo = new HashMap<>();
        for (Sale sale : sales) {
            if (sale.getPagos() != null && !sale.getPagos().isEmpty()) {
                for (Pago pago : sale.getPagos()) {
                    if (pago != null && pago.getFormaDePago() != null && pago.getMonto() != null) {
                        totalPorMetodo.merge(pago.getFormaDePago(), pago.getMonto(), Double::sum);
                    }
                }
            }
        }
        
        // Contar cantidad de ventas por método de pago
        Map<String, Integer> ventasPorMetodo = new HashMap<>();
        for (Sale sale : sales) {
            if (sale.getPagos() != null && !sale.getPagos().isEmpty()) {
                for (Pago pago : sale.getPagos()) {
                    if (pago != null && pago.getFormaDePago() != null && pago.getMonto() != null) {
                        ventasPorMetodo.merge(pago.getFormaDePago(), 1, Integer::sum);
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
        
        List<Sale> allSales = getSalesByDateRange(startDate, endDate, authentication);
        
        // Aplicar filtro de local si se especificó
        if (localFilter != null && !localFilter.isEmpty()) {
            // Validate localId exists
            try {
                localService.validateActiveLocalId(localFilter);
                final String localId = localFilter;
                allSales = allSales.stream()
                        .filter(sale -> localId.equals(sale.getLocalId()))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Si el local no es válido, devolver lista vacía
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
                    
                    double total = ventas.stream()
                            .mapToDouble(Sale::getTotalCLP)
                            .sum();
                    
                    Map<String, Object> vendedorStats = new HashMap<>();
                    vendedorStats.put("vendedor", vendedor);
                    vendedorStats.put("cantidadVentas", ventas.size());
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
        
        // Obtener ventas del mes actual, ya filtradas por locales autorizados
        List<Sale> allSales = getSalesByDateRange(startOfMonth, endOfMonth, authentication);
        
        // Filtrar ventas que no tienen local asignado (datos inconsistentes)
        allSales = allSales.stream()
                .filter(sale -> sale.getLocalId() != null)
                .collect(Collectors.toList());
        
        // Aplicar filtro de local si se especificó
        if (localFilter != null && !localFilter.isEmpty()) {
            // Validate localId exists
            try {
                localService.validateActiveLocalId(localFilter);
                final String localId = localFilter;
                allSales = allSales.stream()
                        .filter(sale -> localId.equals(sale.getLocalId()))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Si el local no es válido, devolver lista vacía
                allSales = List.of();
            }
        }
        
        // Agrupar por local (ahora seguro que todos tienen localId != null)
        Map<String, List<Sale>> salesByLocal = allSales.stream()
                .collect(Collectors.groupingBy(Sale::getLocalId));
        
        return salesByLocal.entrySet().stream()
                .map(entry -> {
                    String localId = entry.getKey();
                    List<Sale> ventas = entry.getValue();
                    
                    double totalVentas = ventas.stream()
                            .mapToDouble(Sale::getTotalCLP)
                            .sum();
                    
                    int cantidadVentas = ventas.size();
                    
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
        
        // Descontar stock ahora que la venta fue aprobada
        for (SaleItem item : approvedSale.getItems()) {
            productService.decreaseStock(item.getProductId(), approvedSale.getLocalId(), item.getCantidad());
        }
        
        // Crear comisión ahora que la venta fue aprobada
        try {
            commissionService.createCommission(approvedSale, sale.getVendedor());
        } catch (Exception e) {
            System.err.println("Error creando comisión: " + e.getMessage());
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
                
                SaleItem saleItem = SaleItem.builder()
                        .productId(product.getId())
                        .productName(product.getNombre())
                        .cantidad(itemReq.getCantidad())
                        .precioUnitario(product.getPrecio())
                        .subtotal(product.getPrecio() * itemReq.getCantidad())
                        .build();
                
                newItems.add(saleItem);
                newTotal += saleItem.getSubtotal();
            }
            
            // Descontar el nuevo stock (solo si la venta ya fue aprobada)
            if ("APROBADA".equals(sale.getEstadoAprobacion())) {
                for (SaleItem newItem : newItems) {
                    productService.decreaseStock(newItem.getProductId(), saleLocal, newItem.getCantidad());
                }
                
                // Actualizar o recalcular comisión si es necesario
                // Por simplicidad, eliminar la comisión antigua y crear una nueva
                commissionService.deleteCommissionBySaleId(saleId);
                
                // Crear nueva comisión con la venta actualizada
                sale.setItems(newItems);
                sale.setTotalCLP(newTotal);
                commissionService.createCommission(sale, sale.getVendedor());
            }
            
            sale.setItems(newItems);
            sale.setTotalCLP(newTotal);
        }
        
        // Actualizar métodos de pago
        if (request.getPagos() != null && !request.getPagos().isEmpty()) {
            validatePagos(request.getPagos());
            validatePaymentTotal(request.getPagos(), sale.getTotalCLP());
            
            List<Pago> pagos = request.getPagos().stream()
                    .map(pr -> Pago.builder()
                            .formaDePago(pr.getFormaDePago())
                            .monto(pr.getMonto())
                            .build())
                    .collect(Collectors.toList());
            sale.setPagos(pagos);
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
    private void validatePagos(List<PagoRequest> pagos) {
        if (pagos == null || pagos.isEmpty()) {
            throw new BadRequestException("Debe incluir al menos un método de pago");
        }
        
        Set<String> metodosValidos = Set.of("EFECTIVO", "TRANSFERENCIA", "DEBITO", "CREDITO");
        
        for (PagoRequest pago : pagos) {
            if (pago.getFormaDePago() == null || pago.getFormaDePago().isEmpty()) {
                throw new BadRequestException("La forma de pago no puede estar vacía");
            }
            
            if (!metodosValidos.contains(pago.getFormaDePago())) {
                throw new BadRequestException("Forma de pago inválida: " + pago.getFormaDePago() + 
                    ". Debe ser uno de: EFECTIVO, TRANSFERENCIA, DEBITO, CREDITO");
            }
            
            if (pago.getMonto() == null || pago.getMonto() <= 0) {
                throw new BadRequestException("El monto de cada pago debe ser mayor a 0");
            }
        }
    }
    
    /**
     * Valida que la suma de los pagos sea igual al total de la venta.
     */
    private void validatePaymentTotal(List<PagoRequest> pagos, double total) {
        double sumaPagos = pagos.stream()
                .mapToDouble(PagoRequest::getMonto)
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
