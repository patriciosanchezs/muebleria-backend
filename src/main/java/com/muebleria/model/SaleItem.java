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
    
    // Precio del producto al momento de la venta
    private Double precioUnitario;
    
    // Subtotal = cantidad * precioUnitario
    private Double subtotal;
}
