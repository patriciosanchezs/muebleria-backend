package com.muebleria.controller;

import com.muebleria.dto.SaleRequest;
import com.muebleria.model.Sale;
import com.muebleria.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SaleController {
    
    private final SaleService saleService;
    
    /**
     * Crear una nueva venta
     */
    @PostMapping
    public ResponseEntity<Sale> createSale(
            @Valid @RequestBody SaleRequest request,
            Authentication authentication) {
        
        String vendedor = authentication != null ? authentication.getName() : "Sistema";
        Sale sale = saleService.createSale(request, vendedor);
        return ResponseEntity.status(HttpStatus.CREATED).body(sale);
    }
    
    /**
     * Obtener todas las ventas
     */
    @GetMapping
    public ResponseEntity<List<Sale>> getAllSales() {
        List<Sale> sales = saleService.getAllSales();
        return ResponseEntity.ok(sales);
    }
    
    /**
     * Obtener una venta por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Sale> getSaleById(@PathVariable String id) {
        Sale sale = saleService.getSaleById(id);
        return ResponseEntity.ok(sale);
    }
    
    /**
     * Obtener ventas por rango de fechas
     * Ejemplo: /sales/by-date?start=2024-01-01T00:00:00&end=2024-12-31T23:59:59
     */
    @GetMapping("/by-date")
    public ResponseEntity<List<Sale>> getSalesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        List<Sale> sales = saleService.getSalesByDateRange(start, end);
        return ResponseEntity.ok(sales);
    }
    
    /**
     * Obtener ventas por método de pago
     * Ejemplo: /sales/by-payment?method=EFECTIVO
     */
    @GetMapping("/by-payment")
    public ResponseEntity<List<Sale>> getSalesByPaymentMethod(@RequestParam String method) {
        List<Sale> sales = saleService.getSalesByPaymentMethod(method);
        return ResponseEntity.ok(sales);
    }
    
    /**
     * Obtener ventas por vendedor
     * Ejemplo: /sales/by-seller?name=juan
     */
    @GetMapping("/by-seller")
    public ResponseEntity<List<Sale>> getSalesByVendedor(@RequestParam String name) {
        List<Sale> sales = saleService.getSalesByVendedor(name);
        return ResponseEntity.ok(sales);
    }
    
    /**
     * Obtener estadísticas de ventas por mes
     * Ejemplo: /sales/stats/month?year=2026&month=4
     */
    @GetMapping("/stats/month")
    public ResponseEntity<Map<String, Object>> getSalesStatsByMonth(
            @RequestParam int year,
            @RequestParam int month) {
        
        Map<String, Object> stats = saleService.getSalesStatsByMonth(year, month);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Obtener estadísticas de ventas por rango de fechas
     * Ejemplo: /sales/stats/range?start=2026-01-01T00:00:00&end=2026-12-31T23:59:59
     */
    @GetMapping("/stats/range")
    public ResponseEntity<Map<String, Object>> getSalesStatsByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        Map<String, Object> stats = saleService.getSalesStatsByRange(start, end);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Obtener todos los despachos pendientes
     * Ejemplo: /sales/deliveries/pending
     */
    @GetMapping("/deliveries/pending")
    public ResponseEntity<List<Sale>> getPendingDeliveries() {
        List<Sale> deliveries = saleService.getPendingDeliveries();
        return ResponseEntity.ok(deliveries);
    }
    
    /**
     * Obtener despachos pendientes por fecha específica
     * Ejemplo: /sales/deliveries/by-date?date=2026-04-15T00:00:00
     */
    @GetMapping("/deliveries/by-date")
    public ResponseEntity<List<Sale>> getDeliveriesByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        
        List<Sale> deliveries = saleService.getDeliveriesByDate(date);
        return ResponseEntity.ok(deliveries);
    }
    
    /**
     * Marcar una venta como entregada
     * Ejemplo: PUT /sales/{id}/deliver
     */
    @PutMapping("/{id}/deliver")
    public ResponseEntity<Sale> markAsDelivered(@PathVariable String id) {
        Sale sale = saleService.markAsDelivered(id);
        return ResponseEntity.ok(sale);
    }
    
    /**
     * Actualizar la fecha de despacho de una venta
     * Ejemplo: PUT /sales/{id}/delivery-date?newDate=2026-04-20T10:00:00
     */
    @PutMapping("/{id}/delivery-date")
    public ResponseEntity<Sale> updateDeliveryDate(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newDate) {
        
        Sale sale = saleService.updateDeliveryDate(id, newDate);
        return ResponseEntity.ok(sale);
    }
}
