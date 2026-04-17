package com.muebleria.service;

import com.muebleria.model.*;
import com.muebleria.repository.CommissionRepository;
import com.muebleria.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommissionService {
    
    private final CommissionRepository commissionRepository;
    private final ProductService productService;
    private final AuthHelper authHelper;
    private final UserRepository userRepository;
    
    // Comisiones base por canal de venta
    private static final double COMISION_EN_LOCAL = 2500.0;
    private static final double COMISION_ONLINE_BUSINESS = 5000.0;
    private static final double COMISION_ONLINE_SIN_BUSINESS = 10000.0;
    
    // Límite de comisión para productos menores a este precio
    private static final double PRECIO_LIMITE = 140000.0;
    private static final double COMISION_MAXIMA_LIMITE = 5000.0;
    
    /**
     * Calcula la comisión para un producto según el canal de venta y precio
     */
    public double calculateCommissionForProduct(double precioProducto, CanalVenta canalVenta) {
        double comisionBase;
        
        // Determinar comisión base según canal
        switch (canalVenta) {
            case EN_LOCAL:
                comisionBase = COMISION_EN_LOCAL;
                break;
            case ONLINE_BUSINESS:
                comisionBase = COMISION_ONLINE_BUSINESS;
                break;
            case ONLINE_SIN_BUSINESS:
                comisionBase = COMISION_ONLINE_SIN_BUSINESS;
                break;
            default:
                comisionBase = COMISION_EN_LOCAL;
        }
        
        // Aplicar límite si el precio del producto es menor a 140,000
        if (precioProducto < PRECIO_LIMITE) {
            return Math.min(comisionBase, COMISION_MAXIMA_LIMITE);
        }
        
        return comisionBase;
    }
    
    /**
     * Crea un registro de comisión para una venta
     */
    public Commission createCommission(Sale sale, String vendedorUsername) {
        List<CommissionItem> commissionItems = new ArrayList<>();
        double totalComision = 0.0;
        
        // Calcular comisión para cada item de la venta
        for (SaleItem saleItem : sale.getItems()) {
            Product product = productService.getProductById(saleItem.getProductId());
            
            // Usar precio con descuento para calcular comisión
            double precioConDescuento = saleItem.getPrecioConDescuento();
            
            double comisionPorProducto = calculateCommissionForProduct(
                precioConDescuento, 
                sale.getCanalVenta()
            );
            
            double comisionItem = comisionPorProducto * saleItem.getCantidad();
            
            CommissionItem commissionItem = CommissionItem.builder()
                .productId(product.getId())
                .productName(product.getNombre())
                .cantidad(saleItem.getCantidad())
                .precioProducto(precioConDescuento) // Precio con descuento
                .canalVenta(sale.getCanalVenta())
                .comisionCalculada(comisionItem)
                .build();
            
            commissionItems.add(commissionItem);
            totalComision += comisionItem;
        }
        
        // Crear registro de comisión
        Commission commission = Commission.builder()
            .saleId(sale.getId())
            .vendedorUsername(vendedorUsername)
            .fechaVenta(sale.getFechaVenta())
            .local(sale.getLocal())
            .items(commissionItems)
            .totalComision(totalComision)
            .build();
        
        return commissionRepository.save(commission);
    }
    
    /**
     * Crea un registro de comisión para un ADMIN_LOCAL
     * Reglas:
     * - Producto < 140.000 CLP → Comisión = 5.000 CLP
     * - Producto >= 140.000 CLP → Comisión = 10.000 CLP
     * Se calcula sobre el precio CON descuento
     */
    public Commission createAdminLocalCommission(Sale sale, String adminLocalId) {
        List<CommissionItem> commissionItems = new ArrayList<>();
        double totalComision = 0.0;
        
        // Calcular comisión para cada item de la venta
        for (SaleItem saleItem : sale.getItems()) {
            Product product = productService.getProductById(saleItem.getProductId());
            
            // Usar precio con descuento para calcular comisión
            double precioConDescuento = saleItem.getPrecioConDescuento();
            
            // Comisión fija según precio del producto CON descuento
            double comisionPorProducto = precioConDescuento < PRECIO_LIMITE ? 5000.0 : 10000.0;
            double comisionItem = comisionPorProducto * saleItem.getCantidad();
            
            CommissionItem commissionItem = CommissionItem.builder()
                .productId(product.getId())
                .productName(product.getNombre())
                .cantidad(saleItem.getCantidad())
                .precioProducto(precioConDescuento) // Precio con descuento
                .canalVenta(sale.getCanalVenta())
                .comisionCalculada(comisionItem)
                .build();
            
            commissionItems.add(commissionItem);
            totalComision += comisionItem;
        }
        
        // Crear registro de comisión
        Commission commission = Commission.builder()
            .saleId(sale.getId())
            .vendedorUsername(adminLocalId) // Usar ID del ADMIN_LOCAL
            .fechaVenta(sale.getFechaVenta())
            .local(sale.getLocal())
            .items(commissionItems)
            .totalComision(totalComision)
            .build();
        
        return commissionRepository.save(commission);
    }
    
    /**
     * Obtiene las comisiones de un vendedor
     */
    public List<Commission> getCommissionsByVendedor(String vendedorUsername) {
        return commissionRepository.findByVendedorUsername(vendedorUsername);
    }
    
    /**
     * Obtiene las comisiones de un vendedor en un periodo
     */
    public List<Commission> getCommissionsByVendedorAndPeriod(String vendedorUsername, LocalDateTime start, LocalDateTime end) {
        return commissionRepository.findByVendedorUsernameAndFechaVentaBetween(vendedorUsername, start, end);
    }
    
    /**
     * Obtiene todas las comisiones en un periodo (filtradas por local autorizado)
     */
    public List<Commission> getCommissionsByPeriod(LocalDateTime start, LocalDateTime end, org.springframework.security.core.Authentication authentication) {
        List<Commission> commissions = commissionRepository.findByFechaVentaBetween(start, end);
        return filterByAuthorizedLocales(commissions, authentication);
    }
    
    /**
     * Calcula el total de comisiones para un vendedor en un periodo
     */
    public double getTotalCommissionsByVendedorAndPeriod(String vendedorUsername, LocalDateTime start, LocalDateTime end) {
        List<Commission> commissions = getCommissionsByVendedorAndPeriod(vendedorUsername, start, end);
        return commissions.stream()
            .mapToDouble(Commission::getTotalComision)
            .sum();
    }
    
    /**
     * Filtra las comisiones por los locales autorizados del usuario.
     * - ADMINISTRADOR: Ve todas las comisiones
     * - ADMIN_LOCAL: Solo ve comisiones de sus locales asignados
     */
    private List<Commission> filterByAuthorizedLocales(List<Commission> commissions, org.springframework.security.core.Authentication authentication) {
        if (authentication == null) {
            return commissions;
        }
        
        List<Local> authorizedLocales = authHelper.getAuthorizedLocales(authentication);
        
        // ADMINISTRADOR ve todas
        if (authHelper.isGlobalAdmin(authentication)) {
            return commissions;
        }
        
        // Filtrar por locales autorizados
        return commissions.stream()
                .filter(commission -> authorizedLocales.contains(commission.getLocal()))
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Elimina la comisión asociada a una venta (para recalcular cuando se edita)
     */
    public void deleteCommissionBySaleId(String saleId) {
        commissionRepository.deleteBySaleId(saleId);
    }
    
    /**
     * Obtiene comisiones agrupadas por usuario para los locales asignados.
     * Incluye resumen por usuario y detalle de comisiones.
     * - ADMIN: Ve todos los locales
     * - ADMIN_LOCAL: Solo ve locales asignados
     */
    public Map<String, Object> getCommissionsByLocalGrouped(
            LocalDateTime start,
            LocalDateTime end,
            String localFilter,
            String usernameFilter,
            org.springframework.security.core.Authentication authentication) {
        
        // Obtener locales autorizados
        List<Local> authorizedLocales = authHelper.isGlobalAdmin(authentication)
                ? Arrays.asList(Local.values())
                : authHelper.getAuthorizedLocales(authentication);
        
        // Filtrar por local si se especifica (debe ser final para usar en lambdas)
        final List<Local> targetLocales;
        if (localFilter != null && !localFilter.isEmpty()) {
            Local local = null;
            try {
                local = Local.valueOf(localFilter);
            } catch (IllegalArgumentException e) {
                // Local inválido, se mantendrá como null
            }
            
            if (local != null && authorizedLocales.contains(local)) {
                targetLocales = Collections.singletonList(local);
            } else {
                // No tiene acceso a este local o local inválido
                targetLocales = Collections.emptyList();
            }
        } else {
            targetLocales = authorizedLocales;
        }
        
        // Obtener todas las comisiones del periodo (o todas si no hay periodo)
        List<Commission> allCommissions;
        if (start != null && end != null) {
            allCommissions = commissionRepository.findByFechaVentaBetween(start, end);
        } else {
            allCommissions = commissionRepository.findAll();
        }
        
        // Filtrar por locales autorizados
        List<Commission> filteredCommissions = allCommissions.stream()
                .filter(c -> targetLocales.contains(c.getLocal()))
                .collect(Collectors.toList());
        
        // Filtrar por usuario si se especifica (debe ser final para usar en lambdas)
        final String finalUsernameFilter = usernameFilter;
        if (finalUsernameFilter != null && !finalUsernameFilter.isEmpty()) {
            // Obtener el usuario para determinar qué identificador usar
            User filterUser = userRepository.findByUsername(finalUsernameFilter).orElse(null);
            
            if (filterUser != null) {
                // Para ADMIN_LOCAL usar ID, para otros roles usar username
                final String identifier = filterUser.getRole() == Role.ADMIN_LOCAL 
                    ? filterUser.getId() 
                    : filterUser.getUsername();
                
                filteredCommissions = filteredCommissions.stream()
                        .filter(c -> c.getVendedorUsername().equals(identifier))
                        .collect(Collectors.toList());
            } else {
                // Usuario no encontrado, retornar lista vacía
                filteredCommissions = Collections.emptyList();
            }
        }
        
        // Obtener todos los usuarios relevantes (VENDEDOR, ENCARGADO_LOCAL, ADMIN_LOCAL)
        List<User> relevantUsers = userRepository.findAll().stream()
                .filter(u -> {
                    Role role = u.getRole();
                    return role == Role.VENDEDOR || 
                           role == Role.ENCARGADO_LOCAL || 
                           role == Role.ADMIN_LOCAL;
                })
                .filter(u -> {
                    // Filtrar por locales: debe tener al menos un local en común con targetLocales
                    if (u.getRole() == Role.ADMIN_LOCAL) {
                        // ADMIN_LOCAL: verificar localesConComision
                        return u.getLocalesConComision() != null &&
                               u.getLocalesConComision().stream().anyMatch(targetLocales::contains);
                    } else {
                        // VENDEDOR, ENCARGADO_LOCAL: verificar locales
                        return u.getLocales() != null &&
                               u.getLocales().stream().anyMatch(targetLocales::contains);
                    }
                })
                .collect(Collectors.toList());
        
        // Agrupar comisiones por usuario
        Map<String, List<Commission>> commissionsByUser = filteredCommissions.stream()
                .collect(Collectors.groupingBy(Commission::getVendedorUsername));
        
        // Crear resumen por usuario
        List<Map<String, Object>> userSummary = relevantUsers.stream()
                .map(user -> {
                    String identifier = user.getRole() == Role.ADMIN_LOCAL ? user.getId() : user.getUsername();
                    List<Commission> userCommissions = commissionsByUser.getOrDefault(identifier, Collections.emptyList());
                    double total = userCommissions.stream()
                            .mapToDouble(Commission::getTotalComision)
                            .sum();

                    Map<String, Object> summary = new HashMap<>();
                    summary.put("userId", user.getId());
                    summary.put("username", user.getUsername());
                    summary.put("email", user.getEmail());
                    summary.put("role", user.getRole().name());
                    summary.put("locales", user.getLocales() != null
                            ? user.getLocales().stream().map(Enum::name).collect(Collectors.toList())
                            : Collections.emptyList());
                    summary.put("localesConComision", user.getLocalesConComision() != null
                            ? user.getLocalesConComision().stream().map(Enum::name).collect(Collectors.toList())
                            : Collections.emptyList());
                    summary.put("totalComision", total);
                    summary.put("cantidadComisiones", userCommissions.size());

                    return summary;
                })
                .sorted((a, b) -> Double.compare((Double) b.get("totalComision"), (Double) a.get("totalComision")))
                .collect(Collectors.toList());

        // Calcular total general
        double totalGeneral = filteredCommissions.stream()
                .mapToDouble(Commission::getTotalComision)
                .sum();
        
        // Crear un mapa de identificador -> usuario para lookup rápido
        Map<String, User> userMap = relevantUsers.stream()
                .collect(Collectors.toMap(
                    user -> user.getRole() == Role.ADMIN_LOCAL ? user.getId() : user.getUsername(),
                    user -> user
                ));
        
        // Enriquecer comisiones con información del usuario
        List<Map<String, Object>> enrichedCommissions = filteredCommissions.stream()
                .map(commission -> {
                    Map<String, Object> commissionMap = new HashMap<>();
                    commissionMap.put("id", commission.getId());
                    commissionMap.put("saleId", commission.getSaleId());
                    commissionMap.put("vendedorUsername", commission.getVendedorUsername()); // ID o username
                    commissionMap.put("fechaVenta", commission.getFechaVenta());
                    commissionMap.put("local", commission.getLocal().name());
                    commissionMap.put("items", commission.getItems());
                    commissionMap.put("totalComision", commission.getTotalComision());
                    
                    // Agregar información del usuario
                    User user = userMap.get(commission.getVendedorUsername());
                    if (user != null) {
                        commissionMap.put("vendedorDisplayName", user.getUsername());
                        commissionMap.put("vendedorRole", user.getRole().name());
                        commissionMap.put("vendedorEmail", user.getEmail());
                    } else {
                        // Usuario no encontrado (posiblemente eliminado)
                        commissionMap.put("vendedorDisplayName", commission.getVendedorUsername());
                        commissionMap.put("vendedorRole", "UNKNOWN");
                        commissionMap.put("vendedorEmail", "");
                    }
                    
                    return commissionMap;
                })
                .collect(Collectors.toList());
        
        // Preparar respuesta
        Map<String, Object> response = new HashMap<>();
        response.put("userSummary", userSummary);
        response.put("commissions", enrichedCommissions);
        response.put("totalGeneral", totalGeneral);
        response.put("cantidadTotal", filteredCommissions.size());
        response.put("localesAutorizados", targetLocales.stream().map(Enum::name).collect(Collectors.toList()));
        
        return response;
    }
}
