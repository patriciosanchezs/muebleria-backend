package com.muebleria.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class AuthResponse {
    
    private String token;
    
    private String type = "Bearer";
    
    private String username;
    
    private String email;
    
    private String role;
    
    private List<String> locales;
    
    public AuthResponse(String token, String username, String email, String role, List<String> locales) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.role = role;
        this.locales = locales;
    }
}
