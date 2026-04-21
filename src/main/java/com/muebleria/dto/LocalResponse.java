package com.muebleria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuesta de Local
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalResponse {
    private String id;
    private String codigo;
    private String nombre;
    private String direccion;
    private String telefono;
    private String email;
    private String ciudad;
    private String region;
    private boolean activo;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
