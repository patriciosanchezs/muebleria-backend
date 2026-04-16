package com.muebleria.controller;

import com.muebleria.dto.CreateUserRequest;
import com.muebleria.dto.UserResponse;
import com.muebleria.model.Role;
import com.muebleria.model.User;
import com.muebleria.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminController {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Crear un nuevo usuario (solo ADMINISTRADOR).
     */
    @PostMapping("/users")
    public ResponseEntity<?> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        // Verificar que el username no exista
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body("El nombre de usuario ya está en uso");
        }
        
        // Verificar que el email no exista
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body("El correo electrónico ya está en uso");
        }
        
        Role role = Role.valueOf(request.getRole());
        
        // Validar locales según rol
        String validationError = validateLocales(role, request.getLocales());
        if (validationError != null) {
            return ResponseEntity.badRequest().body(validationError);
        }
        
        // Convertir locales de String a enum
        List<com.muebleria.model.Local> locales = null;
        if (request.getLocales() != null && !request.getLocales().isEmpty()) {
            try {
                locales = request.getLocales().stream()
                        .map(com.muebleria.model.Local::valueOf)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                return ResponseEntity
                        .badRequest()
                        .body("Local inválido. Valores permitidos: QUILLOTA, COQUIMBO, MUEBLES_SANCHEZ");
            }
        }
        
        // Crear el nuevo usuario
        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .locales(locales != null ? locales : new ArrayList<>())
                .createdBy(currentUser.getUsername())
                .active(true)
                .build();
        
        User savedUser = userRepository.save(newUser);
        
        UserResponse response = UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .locales(savedUser.getLocales() != null 
                        ? savedUser.getLocales().stream().map(Enum::name).collect(Collectors.toList())
                        : new ArrayList<>())
                .active(savedUser.isActive())
                .createdAt(savedUser.getCreatedAt())
                .createdBy(savedUser.getCreatedBy())
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Validar que los locales sean correctos según el rol.
     */
    private String validateLocales(Role role, List<String> locales) {
        if (role == Role.ADMINISTRADOR) {
            // ADMINISTRADOR no necesita locales (acceso total)
            return null;
        }
        
        if (role == Role.FLETERO) {
            if (locales == null || locales.size() != 1) {
                return "El FLETERO debe tener exactamente 1 local asignado";
            }
        }
        
        if (role == Role.ADMIN_LOCAL || role == Role.VENDEDOR) {
            if (locales == null || locales.isEmpty()) {
                return "El " + role.name() + " debe tener al menos 1 local asignado";
            }
        }
        
        return null;
    }
    
    /**
     * Listar todos los usuarios (solo ADMINISTRADOR).
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .locales(user.getLocales() != null 
                                ? user.getLocales().stream().map(Enum::name).collect(Collectors.toList())
                                : new ArrayList<>())
                        .active(user.isActive())
                        .createdAt(user.getCreatedAt())
                        .createdBy(user.getCreatedBy())
                        .build())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(users);
    }
    
    /**
     * Activar/desactivar un usuario (solo ADMINISTRADOR).
     */
    @PatchMapping("/users/{id}/toggle-active")
    public ResponseEntity<?> toggleUserActive(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        user.setActive(!user.isActive());
        User updatedUser = userRepository.save(user);
        
        UserResponse response = UserResponse.builder()
                .id(updatedUser.getId())
                .username(updatedUser.getUsername())
                .email(updatedUser.getEmail())
                .role(updatedUser.getRole().name())
                .locales(updatedUser.getLocales() != null 
                        ? updatedUser.getLocales().stream().map(Enum::name).collect(Collectors.toList())
                        : new ArrayList<>())
                .active(updatedUser.isActive())
                .createdAt(updatedUser.getCreatedAt())
                .createdBy(updatedUser.getCreatedBy())
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Eliminar un usuario (solo ADMINISTRADOR).
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id, @AuthenticationPrincipal UserDetails currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // No permitir que se elimine a sí mismo
        if (user.getUsername().equals(currentUser.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body("No puedes eliminar tu propia cuenta");
        }
        
        userRepository.delete(user);
        return ResponseEntity.ok("Usuario eliminado exitosamente");
    }
    
    /**
     * Actualizar un usuario (solo ADMINISTRADOR).
     */
    @PatchMapping("/users/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable String id,
            @RequestBody CreateUserRequest request) {
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Actualizar campos si están presentes
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body("El nombre de usuario ya está en uso");
            }
            user.setUsername(request.getUsername());
        }
        
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("El correo electrónico ya está en uso");
            }
            user.setEmail(request.getEmail());
        }
        
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        if (request.getRole() != null) {
            Role newRole = Role.valueOf(request.getRole());
            user.setRole(newRole);
            
            // Validar locales si se cambia el rol
            String validationError = validateLocales(newRole, request.getLocales());
            if (validationError != null) {
                return ResponseEntity.badRequest().body(validationError);
            }
        }
        
        // Actualizar locales si están presentes
        if (request.getLocales() != null) {
            try {
                List<com.muebleria.model.Local> locales = request.getLocales().stream()
                        .map(com.muebleria.model.Local::valueOf)
                        .collect(Collectors.toList());
                user.setLocales(locales);
            } catch (IllegalArgumentException e) {
                return ResponseEntity
                        .badRequest()
                        .body("Local inválido. Valores permitidos: QUILLOTA, COQUIMBO, MUEBLES_SANCHEZ");
            }
        }
        
        User updatedUser = userRepository.save(user);
        
        UserResponse response = UserResponse.builder()
                .id(updatedUser.getId())
                .username(updatedUser.getUsername())
                .email(updatedUser.getEmail())
                .role(updatedUser.getRole().name())
                .locales(updatedUser.getLocales() != null 
                        ? updatedUser.getLocales().stream().map(Enum::name).collect(Collectors.toList())
                        : new ArrayList<>())
                .active(updatedUser.isActive())
                .createdAt(updatedUser.getCreatedAt())
                .createdBy(updatedUser.getCreatedBy())
                .build();
        
        return ResponseEntity.ok(response);
    }
}
