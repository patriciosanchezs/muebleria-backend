package com.muebleria.controller;

import com.muebleria.dto.ExpenseRequest;
import com.muebleria.dto.ExpenseResponse;
import com.muebleria.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<ExpenseResponse> createExpense(
            @Valid @RequestBody ExpenseRequest request,
            Authentication authentication) {
        ExpenseResponse response = expenseService.createExpense(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<List<ExpenseResponse>> getExpenses(
            @RequestParam(required = false) String localId,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String metodoPago,
            Authentication authentication) {
        List<ExpenseResponse> expenses = expenseService.getExpenses(
                localId, categoria, fechaDesde, fechaHasta, metodoPago, authentication);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<ExpenseResponse> getExpenseById(
            @PathVariable String id,
            Authentication authentication) {
        ExpenseResponse response = expenseService.getExpenseById(id, authentication);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable String id,
            @Valid @RequestBody ExpenseRequest request,
            Authentication authentication) {
        ExpenseResponse response = expenseService.updateExpense(id, request, authentication);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable String id,
            Authentication authentication) {
        expenseService.deleteExpense(id, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<Map<String, Object>> getExpenseStats(
            @RequestParam(required = false) String localId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Authentication authentication) {
        Map<String, Object> stats = expenseService.getExpenseStats(localId, fechaDesde, fechaHasta, authentication);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/categorias")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<List<Map<String, String>>> getCategorias() {
        return ResponseEntity.ok(expenseService.getCategorias());
    }
}
