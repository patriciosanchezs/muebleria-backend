package com.muebleria.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Set;

@Data
@AllArgsConstructor
public class AuthResponse {
    
    private String token;
    
    private String type = "Bearer";
    
    private String username;
    
    private String email;
    
    private Set<String> roles;
    
    public AuthResponse(String token, String username, String email, Set<String> roles) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }
}
