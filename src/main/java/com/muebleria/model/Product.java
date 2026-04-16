package com.muebleria.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
    
    // Stock por local: Map<Local, Integer>
    @Builder.Default
    private Map<Local, Integer> stockPorLocal = new HashMap<>();
    
    private String imageUrl;
    
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    
    private LocalDateTime fechaActualizacion;
    
    @Builder.Default
    private boolean disponible = true;
    
    // Helper methods para gestionar stock
    public Integer getStock(Local local) {
        return stockPorLocal.getOrDefault(local, 0);
    }
    
    public void setStock(Local local, Integer cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        stockPorLocal.put(local, cantidad);
    }
    
    public void reducirStock(Local local, Integer cantidad) {
        Integer stockActual = getStock(local);
        if (stockActual < cantidad) {
            throw new IllegalArgumentException("Stock insuficiente en " + local.name());
        }
        stockPorLocal.put(local, stockActual - cantidad);
    }
    
    public Integer getStockTotal() {
        return stockPorLocal.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }
}
