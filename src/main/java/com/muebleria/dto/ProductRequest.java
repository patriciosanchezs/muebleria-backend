package com.muebleria.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
public class ProductRequest {
    
    @NotBlank(message = "El nombre es requerido")
    private String nombre;
    
    private String descripcion;
    
    @NotNull(message = "El precio es requerido")
    @Min(value = 0, message = "El precio debe ser mayor o igual a 0")
    private Double precio;
    
    @NotBlank(message = "La categoría es requerida")
    private String categoria;
    
    @NotNull(message = "El stock por local es requerido")
    private Map<String, Integer> stockPorLocal; // Key: nombre del local (QUILLOTA, COQUIMBO, MUEBLES_SANCHEZ)
    
    private String imageUrl;
}
