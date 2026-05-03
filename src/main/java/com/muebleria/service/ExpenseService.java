package com.muebleria.service;

import com.muebleria.dto.ExpenseRequest;
import com.muebleria.dto.ExpenseResponse;
import com.muebleria.exception.BadRequestException;
import com.muebleria.exception.ResourceNotFoundException;
import com.muebleria.model.CategoriaGasto;
import com.muebleria.model.Expense;
import com.muebleria.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final AuthHelper authHelper;
    private final LocalService localService;

    /**
     * Crear un nuevo gasto
     */
    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request, Authentication authentication) {
        String username = authentication.getName();

        // Validar que el local pertenezca a los locales autorizados del usuario
        validateLocalAccess(request.getLocalId(), authentication);

        // Validar categoría
        CategoriaGasto categoria;
        try {
            categoria = CategoriaGasto.valueOf(request.getCategoria());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Categoría no válida: " + request.getCategoria());
        }

        // Validar fecha (si no se envía, usar ahora)
        LocalDateTime fecha = request.getFecha() != null ? request.getFecha() : LocalDateTime.now();

        Expense expense = Expense.builder()
                .localId(request.getLocalId())
                .categoria(categoria)
                .descripcion(request.getDescripcion())
                .monto(request.getMonto())
                .fecha(fecha)
                .metodoPago(request.getMetodoPago())
                .comprobante(request.getComprobante())
                .notas(request.getNotas())
                .registradoPor(username)
                .createdAt(LocalDateTime.now())
                .build();

        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    /**
     * Actualizar un gasto existente
     */
    @Transactional
    public ExpenseResponse updateExpense(String id, ExpenseRequest request, Authentication authentication) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado con ID: " + id));

        // Validar acceso al local actual y al nuevo local
        validateLocalAccess(expense.getLocalId(), authentication);
        if (!expense.getLocalId().equals(request.getLocalId())) {
            validateLocalAccess(request.getLocalId(), authentication);
        }

        // Validar categoría
        CategoriaGasto categoria;
        try {
            categoria = CategoriaGasto.valueOf(request.getCategoria());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Categoría no válida: " + request.getCategoria());
        }

        expense.setLocalId(request.getLocalId());
        expense.setCategoria(categoria);
        expense.setDescripcion(request.getDescripcion());
        expense.setMonto(request.getMonto());
        if (request.getFecha() != null) {
            expense.setFecha(request.getFecha());
        }
        expense.setMetodoPago(request.getMetodoPago());
        expense.setComprobante(request.getComprobante());
        expense.setNotas(request.getNotas());
        expense.setUpdatedAt(LocalDateTime.now());

        Expense updated = expenseRepository.save(expense);
        return toResponse(updated);
    }

    /**
     * Obtener un gasto por ID
     */
    public ExpenseResponse getExpenseById(String id, Authentication authentication) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado con ID: " + id));

        // Validar acceso
        validateLocalAccess(expense.getLocalId(), authentication);

        return toResponse(expense);
    }

    /**
     * Listar gastos con filtros (por local, categoría, rango de fechas, método de pago)
     */
    public List<ExpenseResponse> getExpenses(String localId, String categoria,
                                              LocalDate fechaDesde, LocalDate fechaHasta,
                                              String metodoPago,
                                              Authentication authentication) {
        List<String> authorizedLocales = authHelper.getAuthorizedLocales(authentication);

        if (authorizedLocales.isEmpty()) {
            return List.of();
        }

        // Si se filtra por un local específico, validar acceso
        if (localId != null && !localId.isEmpty()) {
            validateLocalAccess(localId, authentication);
            authorizedLocales = List.of(localId);
        }

        // Parsear categoría
        CategoriaGasto categoriaEnum = null;
        if (categoria != null && !categoria.isEmpty()) {
            try {
                categoriaEnum = CategoriaGasto.valueOf(categoria);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Categoría no válida: " + categoria);
            }
        }

        // Calcular rango de fechas
        LocalDateTime fechaInicio = null;
        LocalDateTime fechaFin = null;
        if (fechaDesde != null) {
            fechaInicio = fechaDesde.atStartOfDay();
        }
        if (fechaHasta != null) {
            fechaFin = fechaHasta.atTime(23, 59, 59);
        }

        List<Expense> expenses;

        // Combinar filtros
        if (categoriaEnum != null && fechaInicio != null && fechaFin != null) {
            expenses = expenseRepository.findByLocalIdInAndCategoriaAndFechaBetween(
                    authorizedLocales, categoriaEnum, fechaInicio, fechaFin);
        } else if (categoriaEnum != null) {
            expenses = expenseRepository.findByLocalIdInAndCategoria(authorizedLocales, categoriaEnum);
        } else if (fechaInicio != null && fechaFin != null) {
            expenses = expenseRepository.findByLocalIdInAndFechaBetween(authorizedLocales, fechaInicio, fechaFin);
        } else {
            expenses = expenseRepository.findByLocalIdIn(authorizedLocales);
        }

        // Filtrar por método de pago si se especifica
        if (metodoPago != null && !metodoPago.isEmpty()) {
            String finalMetodoPago = metodoPago;
            expenses = expenses.stream()
                    .filter(e -> finalMetodoPago.equals(e.getMetodoPago()))
                    .collect(Collectors.toList());
        }

        // Ordenar por fecha descendente
        expenses.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));

        return expenses.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Eliminar un gasto
     */
    @Transactional
    public void deleteExpense(String id, Authentication authentication) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado con ID: " + id));

        // Validar acceso
        validateLocalAccess(expense.getLocalId(), authentication);

        expenseRepository.delete(expense);
    }

    /**
     * Obtener estadísticas de gastos
     */
    public Map<String, Object> getExpenseStats(String localId, LocalDate fechaDesde,
                                                LocalDate fechaHasta,
                                                Authentication authentication) {
        List<String> authorizedLocales = authHelper.getAuthorizedLocales(authentication);

        if (authorizedLocales.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("totalGastos", 0.0);
            empty.put("cantidadGastos", 0);
            empty.put("porCategoria", Map.of());
            empty.put("porLocal", Map.of());
            empty.put("porMetodoPago", Map.of());
            return empty;
        }

        // Si se filtra por un local específico
        if (localId != null && !localId.isEmpty()) {
            validateLocalAccess(localId, authentication);
            authorizedLocales = List.of(localId);
        }

        // Calcular rango de fechas (por defecto: mes actual)
        LocalDateTime fechaInicio;
        LocalDateTime fechaFin;
        if (fechaDesde != null && fechaHasta != null) {
            fechaInicio = fechaDesde.atStartOfDay();
            fechaFin = fechaHasta.atTime(23, 59, 59);
        } else {
            LocalDate now = LocalDate.now();
            fechaInicio = now.withDayOfMonth(1).atStartOfDay();
            fechaFin = now.atTime(23, 59, 59);
        }

        List<Expense> expenses = expenseRepository.findByLocalIdInAndFechaBetween(
                authorizedLocales, fechaInicio, fechaFin);

        double total = expenses.stream().mapToDouble(Expense::getMonto).sum();

        // Por categoría
        Map<String, Double> porCategoria = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategoria().name(),
                        Collectors.summingDouble(Expense::getMonto)
                ));

        // Por local
        Map<String, Double> porLocal = new LinkedHashMap<>();
        Map<String, Long> countPorLocal = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getLocalId, Collectors.counting()));
        Map<String, Double> montoPorLocal = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getLocalId, Collectors.summingDouble(Expense::getMonto)));

        for (String locId : montoPorLocal.keySet()) {
            String nombre = localService.getLocalEntityById(locId).getNombre();
            porLocal.put(nombre, montoPorLocal.get(locId));
        }

        // Por método de pago
        Map<String, Double> porMetodoPago = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getMetodoPago() != null ? e.getMetodoPago() : "SIN_METODO",
                        Collectors.summingDouble(Expense::getMonto)
                ));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalGastos", total);
        stats.put("cantidadGastos", expenses.size());
        stats.put("porCategoria", porCategoria);
        stats.put("porLocal", porLocal);
        stats.put("porMetodoPago", porMetodoPago);

        return stats;
    }

    /**
     * Obtener las categorías disponibles
     */
    public List<Map<String, String>> getCategorias() {
        return Arrays.stream(CategoriaGasto.values())
                .map(cat -> {
                    Map<String, String> map = new LinkedHashMap<>();
                    map.put("value", cat.name());
                    map.put("label", formatCategoriaLabel(cat));
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Formatear el label de la categoría para mostrar en UI
     */
    private String formatCategoriaLabel(CategoriaGasto cat) {
        return switch (cat) {
            case ARRIENDO -> "Arriendo";
            case SERVICIOS_BASICOS -> "Servicios Básicos";
            case SUELDOS -> "Sueldos";
            case INSUMOS -> "Insumos";
            case MANTENCION -> "Mantención";
            case MARKETING -> "Marketing";
            case TRANSPORTE -> "Transporte";
            case OTROS -> "Otros";
        };
    }

    /**
     * Validar que el usuario tenga acceso al local
     */
    private void validateLocalAccess(String localId, Authentication authentication) {
        List<String> authorizedLocales = authHelper.getAuthorizedLocales(authentication);

        if (!authorizedLocales.contains(localId)) {
            throw new BadRequestException("No tiene permiso para acceder a los gastos de este local");
        }
    }

    /**
     * Convertir entidad a DTO de respuesta
     */
    private ExpenseResponse toResponse(Expense expense) {
        String localNombre = "Desconocido";
        try {
            localNombre = localService.getLocalEntityById(expense.getLocalId()).getNombre();
        } catch (Exception ignored) {
        }

        return ExpenseResponse.builder()
                .id(expense.getId())
                .localId(expense.getLocalId())
                .localNombre(localNombre)
                .categoria(expense.getCategoria().name())
                .descripcion(expense.getDescripcion())
                .monto(expense.getMonto())
                .fecha(expense.getFecha())
                .metodoPago(expense.getMetodoPago())
                .comprobante(expense.getComprobante())
                .notas(expense.getNotas())
                .registradoPor(expense.getRegistradoPor())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}
