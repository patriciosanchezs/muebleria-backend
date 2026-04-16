package com.muebleria.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionItem {
    private String productId;
    private String productName;
    private int cantidad;
    private double precioProducto;
    private CanalVenta canalVenta;
    private double comisionCalculada;
}
