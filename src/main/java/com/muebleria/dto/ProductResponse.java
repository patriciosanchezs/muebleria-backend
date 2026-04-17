package com.muebleria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    
    private String id;
    private String nombre;
    private String descripcion;
    private Double precio;
    private String categoria;
    private String local; // VALDIVIA, OSORNO, CHILOE
    private Integer stock;
    private String imageUrl;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private boolean disponible;
}
