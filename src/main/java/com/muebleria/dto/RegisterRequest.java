package com.muebleria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    
    @NotBlank(message = "El username es requerido")
    @Size(min = 3, max = 20, message = "El username debe tener entre 3 y 20 caracteres")
    private String username;
    
    @NotBlank(message = "El email es requerido")
    @Email(message = "Email inválido")
    private String email;
    
    @NotBlank(message = "El password es requerido")
    @Size(min = 6, message = "El password debe tener al menos 6 caracteres")
    private String password;
}
