package com.muebleria.service;

import com.muebleria.dto.SaleItemRequest;
import com.muebleria.dto.SaleRequest;
import com.muebleria.exception.BadRequestException;
import com.muebleria.exception.ResourceNotFoundException;
import com.muebleria.model.CanalVenta;
import com.muebleria.model.Local;
import com.muebleria.model.Product;
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
    
    /**
     * Crea una nueva venta, valida stock por local y descuenta productos del local correcto.
     */
    @Transactional
    public Sale createSale(SaleRequest request, String vendedor) {
        // 1. Validar que el local esté especificado
        if (request.getLocal() == null || request.getLocal().isEmpty()) {
            throw new BadRequestException("Debe especificar el local de la venta");
        }
        
        Local local;
        try {
            local = Local.valueOf(request.getLocal());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Local inválido: " + request.getLocal());
        }
        
        // Validar y convertir canal de venta
        CanalVenta canalVenta;
        try {
            canalVenta = CanalVenta.valueOf(request.getCanalVenta());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Canal de venta inválido: " + request.getCanalVenta());
        }
        
        // 2. Validar que todos los productos tengan stock suficiente en el local especificado
        validateStockByLocal(request.getItems(), local);
        
        // 3. Crear items de la venta y calcular total
        List<SaleItem> saleItems = new ArrayList<>();
        double totalCLP = 0.0;
        
        for (SaleItemRequest itemRequest : request.getItems()) {
            Product product = productService.getProductById(itemRequest.getProductId());
            
            double subtotal = product.getPrecio() * itemRequest.getCantidad();
            
            SaleItem saleItem = SaleItem.builder()
                    .productId(product.getId())
                    .productName(product.getNombre())
                    .cantidad(itemRequest.getCantidad())
                    .precioUnitario(product.getPrecio())
                    .subtotal(subtotal)
                    .build();
            
            saleItems.add(saleItem);
            totalCLP += subtotal;
        }
        
        // 4. Crear la venta
        Sale.SaleBuilder saleBuilder = Sale.builder()
                .items(saleItems)
                .totalCLP(totalCLP)
                .metodoPago(request.getMetodoPago())
                .vendedor(vendedor)
                .clienteNombre(request.getClienteNombre())
                .clienteDireccion(request.getClienteDireccion())
                .clienteCorreo(request.getClienteCorreo())
                .clienteTelefono(request.getClienteTelefono())
                .tipoEntrega(request.getTipoEntrega())
                .local(local)
                .canalVenta(canalVenta)
                .notas(request.getNotas())
                .fechaVenta(LocalDateTime.now());
        
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
        }
        
        Sale sale = saleBuilder.build();
        
        Sale savedSale = saleRepository.save(sale);
        
        // 5. Descontar stock de productos en el local específico
        for (SaleItemRequest itemRequest : request.getItems()) {
            productService.decreaseStock(itemRequest.getProductId(), local, itemRequest.getCantidad());
        }
        
        // 6. Crear comisión si el usuario es VENDEDOR
        try {
            User user = userRepository.findByUsername(vendedor).orElse(null);
            if (user != null && "VENDEDOR".equals(user.getRole().name())) {
                commissionService.createCommission(savedSale, vendedor);
            }
        } catch (Exception e) {
            // Si falla la creación de comisión, no afecta la venta
            // Log del error (en producción usar logger)
            System.err.println("Error creando comisión: " + e.getMessage());
        }
        
        return savedSale;
    }
    
    /**
     * Valida que haya stock suficiente en un local específico para todos los productos.
     */
    private void validateStockByLocal(List<SaleItemRequest> items, Local local) {
        List<String> errors = new ArrayList<>();
        
        for (SaleItemRequest item : items) {
            try {
                Product product = productService.getProductById(item.getProductId());
                
                if (!product.isDisponible()) {
                    errors.add(String.format("Producto '%s' no está disponible", product.getNombre()));
                    continue;
                }
                
                Integer stockEnLocal = product.getStock(local);
                if (stockEnLocal < item.getCantidad()) {
                    errors.add(String.format(
                        "Stock insuficiente para '%s' en %s. Disponible: %d, Requerido: %d",
                        product.getNombre(), local.name(), stockEnLocal, item.getCantidad()
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
     * Obtiene ventas por método de pago (filtradas por locales autorizados).
     */
    public List<Sale> getSalesByPaymentMethod(String metodoPago, org.springframework.security.core.Authentication authentication) {
        List<Sale> sales = saleRepository.findByMetodoPago(metodoPago);
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
            try {
                Local filterLocal = Local.valueOf(localFilter);
                sales = sales.stream()
                        .filter(sale -> sale.getLocal() == filterLocal)
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
        
        // Ventas por método de pago
        Map<String, Long> ventasPorMetodo = sales.stream()
                .collect(Collectors.groupingBy(Sale::getMetodoPago, Collectors.counting()));
        
        // Total por método de pago
        Map<String, Double> totalPorMetodo = sales.stream()
                .collect(Collectors.groupingBy(
                    Sale::getMetodoPago,
                    Collectors.summingDouble(Sale::getTotalCLP)
                ));
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("periodo", String.format("%d-%02d", year, month));
        stats.put("totalVentas", totalVentas);
        stats.put("cantidadVentas", cantidadVentas);
        stats.put("promedioVenta", cantidadVentas > 0 ? totalVentas / cantidadVentas : 0);
        stats.put("ventasPorMetodo", ventasPorMetodo);
        stats.put("totalPorMetodo", totalPorMetodo);
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
            try {
                Local filterLocal = Local.valueOf(localFilter);
                sales = sales.stream()
                        .filter(sale -> sale.getLocal() == filterLocal)
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
        
        // Ventas por método de pago
        Map<String, Long> ventasPorMetodo = sales.stream()
                .collect(Collectors.groupingBy(Sale::getMetodoPago, Collectors.counting()));
        
        // Total por método de pago
        Map<String, Double> totalPorMetodo = sales.stream()
                .collect(Collectors.groupingBy(
                    Sale::getMetodoPago,
                    Collectors.summingDouble(Sale::getTotalCLP)
                ));
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("fechaInicio", start);
        stats.put("fechaFin", end);
        stats.put("totalVentas", totalVentas);
        stats.put("cantidadVentas", cantidadVentas);
        stats.put("promedioVenta", cantidadVentas > 0 ? totalVentas / cantidadVentas : 0);
        stats.put("ventasPorMetodo", ventasPorMetodo);
        stats.put("totalPorMetodo", totalPorMetodo);
        
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
            try {
                Local filterLocal = Local.valueOf(localFilter);
                completedDeliveries = completedDeliveries.stream()
                        .filter(sale -> sale.getLocal() == filterLocal)
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
            try {
                Local filterLocal = Local.valueOf(localFilter);
                allSales = allSales.stream()
                        .filter(sale -> sale.getLocal() == filterLocal)
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
        
        List<Local> authorizedLocales = authHelper.getAuthorizedLocales(authentication);
        
        // ADMINISTRADOR ve todas
        if (authHelper.isGlobalAdmin(authentication)) {
            return sales;
        }
        
        // Filtrar por locales autorizados
        return sales.stream()
                .filter(sale -> authorizedLocales.contains(sale.getLocal()))
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
        
        // Aplicar filtro de local si se especificó
        if (localFilter != null && !localFilter.isEmpty()) {
            try {
                Local filterLocal = Local.valueOf(localFilter);
                allSales = allSales.stream()
                        .filter(sale -> sale.getLocal() == filterLocal)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Si el local no es válido, devolver lista vacía
                allSales = List.of();
            }
        }
        
        // Agrupar por local
        Map<Local, List<Sale>> salesByLocal = allSales.stream()
                .collect(Collectors.groupingBy(Sale::getLocal));
        
        return salesByLocal.entrySet().stream()
                .map(entry -> {
                    Local local = entry.getKey();
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
                    localStats.put("local", local.name());
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
                .collect(Collectors.toList());
        
        // Aplicar filtro de local si se especificó
        if (localFilter != null && !localFilter.isEmpty()) {
            try {
                Local filterLocal = Local.valueOf(localFilter);
                fletes = fletes.stream()
                        .filter(sale -> sale.getLocal() == filterLocal)
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
        
        // Fletes por local
        Map<String, Long> fletesPorLocal = fletes.stream()
                .collect(Collectors.groupingBy(
                    sale -> sale.getLocal().name(),
                    Collectors.counting()
                ));
        
        // Total por local
        Map<String, Double> totalPorLocal = fletes.stream()
                .collect(Collectors.groupingBy(
                    sale -> sale.getLocal().name(),
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
}
