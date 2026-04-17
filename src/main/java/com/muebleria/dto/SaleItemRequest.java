package com.muebleria.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaleItemRequest {
    
    @NotBlank(message = "El ID del producto es requerido")
    private String productId;
    
    @NotNull(message = "La cantidad es requerida")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;
    
    // Descuento opcional por unidad (monto fijo en CLP)
    @Min(value = 0, message = "El descuento no puede ser negativo")
    private Double descuento;
}
