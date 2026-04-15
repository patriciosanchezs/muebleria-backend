package com.muebleria.service;

import com.muebleria.dto.SaleItemRequest;
import com.muebleria.dto.SaleRequest;
import com.muebleria.exception.BadRequestException;
import com.muebleria.exception.ResourceNotFoundException;
import com.muebleria.model.Product;
import com.muebleria.model.Sale;
import com.muebleria.model.SaleItem;
import com.muebleria.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleService {
    
    private final SaleRepository saleRepository;
    private final ProductService productService;
    
    /**
     * Crea una nueva venta, valida stock y descuenta productos.
     */
    @Transactional
    public Sale createSale(SaleRequest request, String vendedor) {
        // 1. Validar que todos los productos tengan stock suficiente
        validateStock(request.getItems());
        
        // 2. Crear items de la venta y calcular total
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
        
        // 3. Crear la venta
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
                .notas(request.getNotas())
                .fechaVenta(LocalDateTime.now());
        
        // Si el tipo de entrega es ENTREGADO, marcar como entregado inmediatamente
        if ("ENTREGADO".equals(request.getTipoEntrega())) {
            saleBuilder.estadoEntrega("ENTREGADO");
        } else if ("DESPACHO".equals(request.getTipoEntrega())) {
            saleBuilder.estadoEntrega("POR_ENTREGAR");
            // Parsear fecha de despacho si se proporcionó
            if (request.getFechaDespacho() != null && !request.getFechaDespacho().isEmpty()) {
                saleBuilder.fechaDespacho(LocalDateTime.parse(request.getFechaDespacho()));
            }
        }
        
        Sale sale = saleBuilder.build();
        
        Sale savedSale = saleRepository.save(sale);
        
        // 4. Descontar stock de productos
        for (SaleItemRequest itemRequest : request.getItems()) {
            productService.decreaseStock(itemRequest.getProductId(), itemRequest.getCantidad());
        }
        
        return savedSale;
    }
    
    /**
     * Valida que haya stock suficiente para todos los productos.
     */
    private void validateStock(List<SaleItemRequest> items) {
        List<String> errors = new ArrayList<>();
        
        for (SaleItemRequest item : items) {
            try {
                Product product = productService.getProductById(item.getProductId());
                
                if (!product.isDisponible()) {
                    errors.add(String.format("Producto '%s' no está disponible", product.getNombre()));
                }
                
                if (product.getStock() < item.getCantidad()) {
                    errors.add(String.format(
                        "Stock insuficiente para '%s'. Disponible: %d, Requerido: %d",
                        product.getNombre(), product.getStock(), item.getCantidad()
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
     * Obtiene todas las ventas.
     */
    public List<Sale> getAllSales() {
        return saleRepository.findAll();
    }
    
    /**
     * Obtiene una venta por ID.
     */
    public Sale getSaleById(String id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id: " + id));
    }
    
    /**
     * Obtiene ventas por rango de fechas.
     */
    public List<Sale> getSalesByDateRange(LocalDateTime start, LocalDateTime end) {
        return saleRepository.findByFechaVentaBetween(start, end);
    }
    
    /**
     * Obtiene ventas por método de pago.
     */
    public List<Sale> getSalesByPaymentMethod(String metodoPago) {
        return saleRepository.findByMetodoPago(metodoPago);
    }
    
    /**
     * Obtiene ventas por vendedor.
     */
    public List<Sale> getSalesByVendedor(String vendedor) {
        return saleRepository.findByVendedor(vendedor);
    }
    
    /**
     * Obtiene estadísticas de ventas por mes.
     */
    public Map<String, Object> getSalesStatsByMonth(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        
        List<Sale> sales = getSalesByDateRange(start, end);
        
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
     * Obtiene estadísticas generales por rango de fechas.
     */
    public Map<String, Object> getSalesStatsByRange(LocalDateTime start, LocalDateTime end) {
        List<Sale> sales = getSalesByDateRange(start, end);
        
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
     * Marca una venta como entregada.
     */
    @Transactional
    public Sale markAsDelivered(String saleId) {
        Sale sale = getSaleById(saleId);
        sale.setEstadoEntrega("ENTREGADO");
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
}
