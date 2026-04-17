package com.muebleria.controller;

import com.muebleria.model.Role;
import com.muebleria.model.User;
import com.muebleria.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador temporal para debugging y corrección de datos
 * ELIMINAR EN PRODUCCIÓN
 */
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {
    
    private final UserRepository userRepository;
    
    /**
     * Endpoint para verificar el estado del usuario admin
     */
    @GetMapping("/check-admin")
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR', 'ADMIN_LOCAL')")
    public ResponseEntity<Map<String, Object>> checkAdmin() {
        User admin = userRepository.findByUsername("admin").orElse(null);
        
        Map<String, Object> response = new HashMap<>();
        if (admin == null) {
            response.put("found", false);
            response.put("message", "Usuario admin no encontrado");
        } else {
            response.put("found", true);
            response.put("username", admin.getUsername());
            response.put("role", admin.getRole().name());
            response.put("locales", admin.getLocales() != null ? 
                admin.getLocales().stream().map(Enum::name).toList() : new ArrayList<>());
            response.put("active", admin.isActive());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Endpoint para corregir el usuario admin
     */
    @PostMapping("/fix-admin")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> fixAdmin() {
        User admin = userRepository.findByUsername("admin")
            .orElseThrow(() -> new RuntimeException("Usuario admin no encontrado"));
        
        // Guardar estado anterior
        String roleBefore = admin.getRole().name();
        int localesCountBefore = admin.getLocales() != null ? admin.getLocales().size() : 0;
        
        // Corregir usuario admin
        admin.setRole(Role.ADMINISTRADOR);
        admin.setLocales(new ArrayList<>()); // Admin no debe tener locales específicos
        admin.setSubRoles(new ArrayList<>()); // Admin no debe tener sub-roles
        
        User saved = userRepository.save(admin);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Usuario admin corregido exitosamente");
        response.put("before", Map.of(
            "role", roleBefore,
            "localesCount", localesCountBefore
        ));
        response.put("after", Map.of(
            "role", saved.getRole().name(),
            "localesCount", saved.getLocales().size()
        ));
        response.put("note", "Por favor, cierra sesión y vuelve a iniciar sesión para aplicar los cambios");
        
        return ResponseEntity.ok(response);
    }
}
