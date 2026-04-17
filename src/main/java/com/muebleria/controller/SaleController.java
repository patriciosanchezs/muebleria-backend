package com.muebleria.controller;

import com.muebleria.dto.SaleRequest;
import com.muebleria.model.Sale;
import com.muebleria.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
     * Crear una nueva venta (ADMINISTRADOR, ADMIN_LOCAL, ENCARGADO_LOCAL y VENDEDOR)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL', 'VENDEDOR')")
    public ResponseEntity<Sale> createSale(
            @Valid @RequestBody SaleRequest request,
            Authentication authentication) {
        
        String vendedor = authentication != null ? authentication.getName() : "Sistema";
        Sale sale = saleService.createSale(request, vendedor);
        return ResponseEntity.status(HttpStatus.CREATED).body(sale);
    }
    
    /**
     * Obtener ventas con filtros (ADMINISTRADOR, ADMIN_LOCAL, ENCARGADO_LOCAL y VENDEDOR)
     * Por defecto devuelve solo las ventas del día actual.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL', 'VENDEDOR')")
    public ResponseEntity<List<Sale>> getAllSales(
            @RequestParam(required = false) String local,
            @RequestParam(required = false) String vendedor,
            @RequestParam(required = false) String metodoPago,
            @RequestParam(required = false) String estadoEntrega,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false, defaultValue = "false") Boolean todayOnly,
            @RequestParam(required = false, defaultValue = "false") Boolean allTime,
            Authentication authentication) {
        
        List<Sale> sales = saleService.getSalesWithFilters(
            local, vendedor, metodoPago, estadoEntrega, 
            startDate, endDate, todayOnly, allTime, authentication
        );
        return ResponseEntity.ok(sales);
    }
    
    /**
     * Obtener una venta por ID (ADMINISTRADOR, ADMIN_LOCAL y VENDEDOR)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL', 'VENDEDOR')")
    public ResponseEntity<Sale> getSaleById(@PathVariable String id) {
        Sale sale = saleService.getSaleById(id);
        return ResponseEntity.ok(sale);
    }
    
    /**
     * Obtener ventas por rango de fechas (ADMINISTRADOR, ADMIN_LOCAL y VENDEDOR)
     * Ejemplo: /sales/by-date?start=2024-01-01T00:00:00&end=2024-12-31T23:59:59
     */
    @GetMapping("/by-date")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL', 'VENDEDOR')")
    public ResponseEntity<List<Sale>> getSalesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Authentication authentication) {
        
        List<Sale> sales = saleService.getSalesByDateRange(start, end, authentication);
        return ResponseEntity.ok(sales);
    }
    
    /**
     * Obtener ventas por método de pago (ADMINISTRADOR o ADMIN_LOCAL)
     * Ejemplo: /sales/by-payment?method=EFECTIVO
     */
    @GetMapping("/by-payment")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<List<Sale>> getSalesByPaymentMethod(@RequestParam String method, Authentication authentication) {
        List<Sale> sales = saleService.getSalesByPaymentMethod(method, authentication);
        return ResponseEntity.ok(sales);
    }
    
    /**
     * Obtener ventas por vendedor (ADMINISTRADOR o ADMIN_LOCAL)
     * Ejemplo: /sales/by-seller?name=juan
     */
    @GetMapping("/by-seller")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<List<Sale>> getSalesByVendedor(@RequestParam String name) {
        List<Sale> sales = saleService.getSalesByVendedor(name);
        return ResponseEntity.ok(sales);
    }
    
    /**
     * Obtener mis ventas (VENDEDOR)
     * El vendedor autenticado obtiene sus propias ventas
     */
    @GetMapping("/my-sales")
    @PreAuthorize("hasRole('VENDEDOR')")
    public ResponseEntity<List<Sale>> getMySales(Authentication authentication) {
        String vendedor = authentication.getName();
        List<Sale> sales = saleService.getSalesByVendedor(vendedor);
        return ResponseEntity.ok(sales);
    }
    
    /**
     * Obtener estadísticas de ventas por mes (ADMINISTRADOR o ADMIN_LOCAL)
     * Ejemplo: /sales/stats/month?year=2026&month=4
     */
    @GetMapping("/stats/month")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<Map<String, Object>> getSalesStatsByMonth(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String local,
            Authentication authentication) {
        
        Map<String, Object> stats = saleService.getSalesStatsByMonth(year, month, local, authentication);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Obtener estadísticas de ventas por rango de fechas (ADMINISTRADOR o ADMIN_LOCAL)
     * Ejemplo: /sales/stats/range?start=2026-01-01T00:00:00&end=2026-12-31T23:59:59
     */
    @GetMapping("/stats/range")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<Map<String, Object>> getSalesStatsByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String local,
            Authentication authentication) {
        
        Map<String, Object> stats = saleService.getSalesStatsByRange(start, end, local, authentication);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Obtener todos los despachos pendientes (ADMINISTRADOR, ADMIN_LOCAL y FLETERO)
     * Ejemplo: /sales/deliveries/pending
     */
    @GetMapping("/deliveries/pending")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL', 'FLETERO')")
    public ResponseEntity<List<Sale>> getPendingDeliveries() {
        List<Sale> deliveries = saleService.getPendingDeliveries();
        return ResponseEntity.ok(deliveries);
    }
    
    /**
     * Obtener despachos pendientes por fecha específica (ADMINISTRADOR, ADMIN_LOCAL y FLETERO)
     * Ejemplo: /sales/deliveries/by-date?date=2026-04-15T00:00:00
     */
    @GetMapping("/deliveries/by-date")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL', 'FLETERO')")
    public ResponseEntity<List<Sale>> getDeliveriesByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        
        List<Sale> deliveries = saleService.getDeliveriesByDate(date);
        return ResponseEntity.ok(deliveries);
    }
    
    /**
     * Obtener historial de despachos completados (ADMINISTRADOR, ADMIN_LOCAL y FLETERO)
     * FLETERO solo ve sus propias entregas
     * Ejemplo: /sales/deliveries/completed?start=2026-04-01T00:00:00&end=2026-04-30T23:59:59&local=QUILLOTA
     */
    @GetMapping("/deliveries/completed")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL', 'FLETERO')")
    public ResponseEntity<List<Sale>> getCompletedDeliveries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String local,
            Authentication authentication) {
        
        List<Sale> deliveries = saleService.getCompletedDeliveries(start, end, local, authentication);
        return ResponseEntity.ok(deliveries);
    }
    
    /**
     * Marcar una venta como entregada (ADMINISTRADOR, ADMIN_LOCAL y FLETERO)
     * Ejemplo: PUT /sales/{id}/deliver?montoFlete=5000&metodoPagoFlete=EFECTIVO
     */
    @PutMapping("/{id}/deliver")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL', 'FLETERO')")
    public ResponseEntity<Sale> markAsDelivered(
            @PathVariable String id,
            @RequestParam(required = false) Double montoFlete,
            @RequestParam(required = false) String metodoPagoFlete,
            Authentication authentication) {
        String entregadoPor = authentication != null ? authentication.getName() : "Sistema";
        Sale sale = saleService.markAsDelivered(id, entregadoPor, montoFlete, metodoPagoFlete);
        return ResponseEntity.ok(sale);
    }
    
    /**
     * Actualizar la fecha de despacho de una venta (ADMINISTRADOR, ADMIN_LOCAL y FLETERO)
     * Ejemplo: PUT /sales/{id}/delivery-date?newDate=2026-04-20T10:00:00
     */
    @PutMapping("/{id}/delivery-date")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL', 'FLETERO')")
    public ResponseEntity<Sale> updateDeliveryDate(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newDate) {
        
        Sale sale = saleService.updateDeliveryDate(id, newDate);
        return ResponseEntity.ok(sale);
    }
    
    /**
     * Obtener estadísticas personales del vendedor (VENDEDOR)
     * Retorna ventas del día y del mes del vendedor autenticado
     */
    @GetMapping("/my-stats")
    @PreAuthorize("hasRole('VENDEDOR')")
    public ResponseEntity<Map<String, Object>> getMyStats(Authentication authentication) {
        String vendedor = authentication.getName();
        Map<String, Object> stats = saleService.getVendedorStats(vendedor);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Obtener estadísticas de ventas por cada vendedor (ADMINISTRADOR o ADMIN_LOCAL)
     * Para el dashboard de admin - acepta rango de fechas opcional
     */
    @GetMapping("/stats/by-sellers")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<List<Map<String, Object>>> getStatsBySellers(
            @RequestParam(required = false) String local,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Authentication authentication) {
        List<Map<String, Object>> stats = saleService.getStatsByAllSellers(local, start, end, authentication);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Obtener estadísticas agrupadas por local (ADMINISTRADOR o ADMIN_LOCAL)
     * Para el dashboard - muestra ventas del mes actual por local
     */
    @GetMapping("/stats/by-locales")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<List<Map<String, Object>>> getStatsByLocales(
            @RequestParam(required = false) String local,
            Authentication authentication) {
        List<Map<String, Object>> stats = saleService.getStatsByLocales(local, authentication);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Obtener estadísticas mensuales del año completo (ADMINISTRADOR o ADMIN_LOCAL)
     * Devuelve un mapa con estadísticas de cada mes del año especificado
     */
    @GetMapping("/stats/year")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<Map<String, Map<String, Object>>> getYearStats(
            @RequestParam int year,
            @RequestParam(required = false) String local,
            Authentication authentication) {
        Map<String, Map<String, Object>> stats = saleService.getYearStats(year, local, authentication);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Obtener estadísticas de fletes (ADMINISTRADOR, ADMIN_LOCAL y FLETERO)
     * Ejemplo: GET /sales/stats/freight?start=2026-04-01T00:00:00&end=2026-04-30T23:59:59&local=QUILLOTA
     */
    @GetMapping("/stats/freight")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL', 'FLETERO')")
    public ResponseEntity<Map<String, Object>> getFreightStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String local,
            Authentication authentication) {
        Map<String, Object> stats = saleService.getFreightStats(start, end, local, authentication);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Obtener ventas pendientes de aprobación (ADMINISTRADOR y ADMIN_LOCAL)
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<List<Sale>> getPendingSales(Authentication authentication) {
        List<Sale> pendingSales = saleService.getPendingSales(authentication);
        return ResponseEntity.ok(pendingSales);
    }
    
    /**
     * Obtener ventas aprobadas (ADMINISTRADOR, ADMIN_LOCAL y VENDEDOR)
     */
    @GetMapping("/approved")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL', 'VENDEDOR')")
    public ResponseEntity<List<Sale>> getApprovedSales(Authentication authentication) {
        List<Sale> approvedSales = saleService.getApprovedSales(authentication);
        return ResponseEntity.ok(approvedSales);
    }
    
    /**
     * Aprobar una venta pendiente (ADMINISTRADOR y ADMIN_LOCAL)
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<Sale> approveSale(
            @PathVariable String id,
            Authentication authentication) {
        String approvedBy = authentication != null ? authentication.getName() : "Sistema";
        Sale approvedSale = saleService.approveSale(id, approvedBy);
        return ResponseEntity.ok(approvedSale);
    }
    
    /**
     * Rechazar una venta pendiente (ADMINISTRADOR y ADMIN_LOCAL)
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<Sale> rejectSale(
            @PathVariable String id,
            @RequestParam(required = false) String motivo,
            Authentication authentication) {
        String rejectedBy = authentication != null ? authentication.getName() : "Sistema";
        String reason = motivo != null ? motivo : "No especificado";
        Sale rejectedSale = saleService.rejectSale(id, rejectedBy, reason);
        return ResponseEntity.ok(rejectedSale);
    }
    
    /**
     * Obtener lista de vendedores únicos (ADMINISTRADOR y ADMIN_LOCAL)
     */
    @GetMapping("/vendedores")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<List<String>> getUniqueVendedores(Authentication authentication) {
        List<String> vendedores = saleService.getUniqueVendedores(authentication);
        return ResponseEntity.ok(vendedores);
    }
    
    /**
     * Actualizar una venta existente (ADMINISTRADOR y ADMIN_LOCAL)
     * Solo se pueden actualizar ventas que no hayan sido entregadas ni rechazadas
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<Sale> updateSale(
            @PathVariable String id,
            @Valid @RequestBody com.muebleria.dto.SaleUpdateRequest request,
            Authentication authentication) {
        Sale updatedSale = saleService.updateSale(id, request, authentication);
        return ResponseEntity.ok(updatedSale);
    }

    /**
     * Actualizar método de pago de una venta (incluso si está cerrada)
     * Solo ADMINISTRADOR y ADMIN_LOCAL
     */
    @PatchMapping("/{id}/payment-method")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL')")
    public ResponseEntity<Sale> updatePaymentMethod(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        String metodoPago = request.get("metodoPago");
        if (metodoPago == null || metodoPago.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Sale updatedSale = saleService.updatePaymentMethod(id, metodoPago);
        return ResponseEntity.ok(updatedSale);
    }
}

