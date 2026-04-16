package com.muebleria.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String username;
    
    @Indexed(unique = true)
    private String email;
    
    private String password;
    
    // Rol del usuario: ADMINISTRADOR, ADMIN_LOCAL, VENDEDOR, FLETERO
    @Builder.Default
    private Role role = Role.VENDEDOR;
    
    // Locales asignados al usuario
    // - ADMINISTRADOR: null/vacío (acceso a todos)
    // - ADMIN_LOCAL: 1 o más locales
    // - VENDEDOR: 1 o más locales (puede vender en varios)
    // - FLETERO: exactamente 1 local
    @Builder.Default
    private List<Local> locales = new ArrayList<>();
    
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Builder.Default
    private boolean active = true;
    
    // Usuario que creó esta cuenta (solo para auditoría)
    private String createdBy;
}
