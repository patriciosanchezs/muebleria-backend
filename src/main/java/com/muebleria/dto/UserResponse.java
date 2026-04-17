package com.muebleria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private String role;
    private List<String> locales;
    private List<String> subRoles;
    private List<String> localesConComision; // IDs de locales con comisión para ADMIN_LOCAL
    private boolean active;
    private LocalDateTime createdAt;
    private String createdBy;
}
