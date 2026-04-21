package com.muebleria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class CreateUserRequest {
    
    @NotBlank(message = "El nombre de usuario es requerido")
    @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
    private String username;
    
    @NotBlank(message = "El correo electrónico es requerido")
    @Email(message = "El correo electrónico debe ser válido")
    private String email;
    
    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
    private String password;
    
    @NotBlank(message = "El rol es requerido")
    @Pattern(
        regexp = "ADMINISTRADOR|ADMIN_LOCAL|ENCARGADO_LOCAL|VENDEDOR|VENDEDOR_SIN_COMISION|BODEGUERO|FLETERO",
        message = "El rol debe ser: ADMINISTRADOR, ADMIN_LOCAL, ENCARGADO_LOCAL, VENDEDOR, VENDEDOR_SIN_COMISION, BODEGUERO o FLETERO"
    )
    private String role;
    
    // Locales asignados (requerido para ADMIN_LOCAL, ENCARGADO_LOCAL, VENDEDOR, VENDEDOR_SIN_COMISION, BODEGUERO y FLETERO)
    private List<String> locales;
    
    // Sub-roles (opcional, solo para VENDEDOR, VENDEDOR_SIN_COMISION y ENCARGADO_LOCAL)
    // Valores permitidos: VENDEDOR_LOCAL, ONLINE_CON_BUSINESS, ONLINE_SIN_BUSINESS
    private List<String> subRoles;
    
    // Locales con comisión (opcional, solo para ADMIN_LOCAL)
    // Indica de qué locales el ADMIN_LOCAL recibe comisión de TODAS las ventas
    private List<String> localesConComision;
}
