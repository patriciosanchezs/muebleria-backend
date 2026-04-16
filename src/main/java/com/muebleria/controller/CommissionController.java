package com.muebleria.controller;

import com.muebleria.model.Commission;
import com.muebleria.service.CommissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/commissions")
@RequiredArgsConstructor
public class CommissionController {
    
    private final CommissionService commissionService;
    
    /**
     * Obtener comisiones del vendedor autenticado
     */
    @GetMapping("/my-commissions")
    @PreAuthorize("hasRole('VENDEDOR')")
    public ResponseEntity<List<Commission>> getMyCommissions(Authentication auth) {
        String username = auth.getName();
        List<Commission> commissions = commissionService.getCommissionsByVendedor(username);
        return ResponseEntity.ok(commissions);
    }
    
    /**
     * Obtener comisiones del vendedor autenticado en un periodo
     */
    @GetMapping("/my-commissions/period")
    @PreAuthorize("hasRole('VENDEDOR')")
    public ResponseEntity<Map<String, Object>> getMyCommissionsByPeriod(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        String username = auth.getName();
        List<Commission> commissions = commissionService.getCommissionsByVendedorAndPeriod(username, start, end);
        double total = commissionService.getTotalCommissionsByVendedorAndPeriod(username, start, end);
        
        Map<String, Object> response = new HashMap<>();
        response.put("commissions", commissions);
        response.put("total", total);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtener comisiones de un vendedor específico (admin y admin_local)
     */
    @GetMapping("/user/{username}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL')")
    public ResponseEntity<List<Commission>> getCommissionsByUser(@PathVariable String username) {
        List<Commission> commissions = commissionService.getCommissionsByVendedor(username);
        return ResponseEntity.ok(commissions);
    }
    
    /**
     * Obtener comisiones de un vendedor en un periodo (admin y admin_local)
     */
    @GetMapping("/user/{username}/period")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL')")
    public ResponseEntity<Map<String, Object>> getCommissionsByUserAndPeriod(
            @PathVariable String username,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        List<Commission> commissions = commissionService.getCommissionsByVendedorAndPeriod(username, start, end);
        double total = commissionService.getTotalCommissionsByVendedorAndPeriod(username, start, end);
        
        Map<String, Object> response = new HashMap<>();
        response.put("commissions", commissions);
        response.put("total", total);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtener todas las comisiones en un periodo (admin y admin_local)
     */
    @GetMapping("/period")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL')")
    public ResponseEntity<Map<String, Object>> getCommissionsByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Authentication authentication) {
        
        List<Commission> commissions = commissionService.getCommissionsByPeriod(start, end, authentication);
        double total = commissions.stream()
            .mapToDouble(Commission::getTotalComision)
            .sum();
        
        Map<String, Object> response = new HashMap<>();
        response.put("commissions", commissions);
        response.put("total", total);
        
        return ResponseEntity.ok(response);
    }
}
