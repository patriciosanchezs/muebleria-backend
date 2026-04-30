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
    
    // Local al que pertenece este producto (ID de LocalEntity)
    private String localId;
    
    // Stock del producto en su local
    @Builder.Default
    private Integer stock = 0;
    
    // Stock temporalmente reservado en ventas en curso
    @Builder.Default
    private Integer stockReservado = 0;
    
    private String imageUrl;
    
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    
    private LocalDateTime fechaActualizacion;
    
    @Builder.Default
    private boolean disponible = true;
    
    // Retorna stock disponible (stock - reservado)
    public Integer getStockDisponible() {
        int realStock = stock != null ? stock : 0;
        int reservado = stockReservado != null ? stockReservado : 0;
        return Math.max(0, realStock - reservado);
    }
}
