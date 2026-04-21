package com.muebleria.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

/**
 * Entidad Local - Representa una sucursal/tienda física
 * Reemplaza el enum Local para permitir gestión dinámica de locales
 */
@Document(collection = "locales")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalEntity {
    
    @Id
    private String id;
    
    /**
     * Código único del local (ej: QUILLOTA, COQUIMBO, MUEBLES_SANCHEZ)
     * Se mantiene para retrocompatibilidad con datos existentes
     */
    @Indexed(unique = true)
    private String codigo;
    
    /**
     * Nombre descriptivo del local
     */
    private String nombre;
    
    /**
     * Dirección física del local
     */
    private String direccion;
    
    /**
     * Teléfono de contacto
     */
    private String telefono;
    
    /**
     * Email de contacto del local
     */
    private String email;
    
    /**
     * Ciudad donde está ubicado el local
     */
    private String ciudad;
    
    /**
     * Región del país
     */
    private String region;
    
    /**
     * Indica si el local está activo
     * Los locales inactivos no se pueden usar para nuevas operaciones
     */
    private boolean activo;
    
    /**
     * Fecha de creación del registro
     */
    private LocalDateTime createdAt;
    
    /**
     * Usuario que creó el registro
     */
    private String createdBy;
    
    /**
     * Fecha de última actualización
     */
    private LocalDateTime updatedAt;
    
    /**
     * Usuario que actualizó por última vez
     */
    private String updatedBy;
}
