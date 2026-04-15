package com.muebleria.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "products")
public class Product {
    
    @Id
    private String id;
    
    private String nombre;
    
    private String descripcion;
    
    private Double precio;
    
    private String categoria;
    
    private Integer stock;
    
    private String imageUrl;
    
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    
    private LocalDateTime fechaActualizacion;
    
    @Builder.Default
    private boolean disponible = true;
}
