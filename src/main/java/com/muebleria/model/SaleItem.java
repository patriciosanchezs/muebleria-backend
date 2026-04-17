package com.muebleria.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleItem {
    
    private String productId;
    
    private String productName;
    
    private Integer cantidad;
    
    // Precio del producto al momento de la venta (precio original)
    private Double precioUnitario;
    
    // Descuento aplicado por unidad (monto fijo en CLP)
    @Builder.Default
    private Double descuento = 0.0;
    
    // Usuario que aplicó el descuento (username)
    private String descuentoAplicadoPor;
    
    // Subtotal = cantidad * (precioUnitario - descuento)
    private Double subtotal;
    
    /**
     * Calcula el precio unitario con descuento aplicado
     */
    public Double getPrecioConDescuento() {
        return precioUnitario - (descuento != null ? descuento : 0.0);
    }
    
    /**
     * Calcula el subtotal considerando cantidad y descuento
     */
    public Double calcularSubtotal() {
        return cantidad * getPrecioConDescuento();
    }
}
