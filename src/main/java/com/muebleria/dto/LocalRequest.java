package com.muebleria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para crear o actualizar un local
 */
@Data
public class LocalRequest {
    
    @NotBlank(message = "El código del local es requerido")
    @Pattern(
        regexp = "^[A-Z_]+$",
        message = "El código debe estar en mayúsculas y puede contener guiones bajos"
    )
    @Size(min = 2, max = 50, message = "El código debe tener entre 2 y 50 caracteres")
    private String codigo;
    
    @NotBlank(message = "El nombre del local es requerido")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre;
    
    @Size(max = 200, message = "La dirección no puede exceder 200 caracteres")
    private String direccion;
    
    @Pattern(
        regexp = "^$|^[0-9+\\-\\s()]+$",
        message = "El teléfono solo puede contener números, espacios y los caracteres: + - ( )"
    )
    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String telefono;
    
    @Pattern(
        regexp = "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$",
        message = "El email debe ser válido"
    )
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    private String email;
    
    @Size(max = 100, message = "La ciudad no puede exceder 100 caracteres")
    private String ciudad;
    
    @Size(max = 100, message = "La región no puede exceder 100 caracteres")
    private String region;
}
