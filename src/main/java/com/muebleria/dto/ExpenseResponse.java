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
public class ExpenseResponse {

    private String id;
    private String localId;
    private String localNombre;
    private String categoria;
    private String descripcion;
    private Double monto;
    private LocalDateTime fecha;
    private String metodoPago;
    private String comprobante;
    private String notas;
    private String registradoPor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
