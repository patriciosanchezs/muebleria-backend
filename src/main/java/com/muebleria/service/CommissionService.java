package com.muebleria.service;

import com.muebleria.model.*;
import com.muebleria.repository.CommissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommissionService {
    
    private final CommissionRepository commissionRepository;
    private final ProductService productService;
    private final AuthHelper authHelper;
    
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
            
            double comisionPorProducto = calculateCommissionForProduct(
                product.getPrecio(), 
                sale.getCanalVenta()
            );
            
            double comisionItem = comisionPorProducto * saleItem.getCantidad();
            
            CommissionItem commissionItem = CommissionItem.builder()
                .productId(product.getId())
                .productName(product.getNombre())
                .cantidad(saleItem.getCantidad())
                .precioProducto(product.getPrecio())
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
}
