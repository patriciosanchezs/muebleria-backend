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
    
    // Rol del usuario: ADMINISTRADOR, ADMIN_LOCAL, ENCARGADO_LOCAL, VENDEDOR, BODEGUERO, FLETERO
    @Builder.Default
    private Role role = Role.VENDEDOR;
    
    // Sub-roles para VENDEDOR y ENCARGADO_LOCAL
    // Define los canales de venta disponibles:
    // - VENDEDOR_LOCAL: Vende en local físico
    // - ONLINE_CON_BUSINESS: Vende online con WhatsApp Business
    // - ONLINE_SIN_BUSINESS: Vende online sin business
    // Un usuario puede tener múltiples sub-roles
    @Builder.Default
    private List<SubRol> subRoles = new ArrayList<>();
    
    // Locales asignados al usuario (IDs de LocalEntity)
    // - ADMINISTRADOR: null/vacío (acceso a todos)
    // - ADMIN_LOCAL: 1 o más locales
    // - ENCARGADO_LOCAL: exactamente 1 local (validado en servicio)
    // - VENDEDOR: 1 o más locales (puede vender en varios)
    // - FLETERO: exactamente 1 local
    @Builder.Default
    private List<String> localIds = new ArrayList<>();
    
    // Locales con comisión para ADMIN_LOCAL (IDs de LocalEntity)
    // - Solo aplica para usuarios con rol ADMIN_LOCAL
    // - El ADMIN_LOCAL recibe comisión de TODAS las ventas de estos locales
    // - Puede tener 0, 1 o múltiples locales con comisión
    // - El ADMIN puede modificar esta lista cuando quiera
    @Builder.Default
    private List<String> localesConComisionIds = new ArrayList<>();
    
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Builder.Default
    private boolean active = true;
    
    // Usuario que creó esta cuenta (solo para auditoría)
    private String createdBy;
}
