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
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminController {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.muebleria.service.LocalService localService;
    
    /**
     * Crear un nuevo usuario (solo ADMINISTRADOR).
     */
    @PostMapping("/users")
    public ResponseEntity<?> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        request.setUsername(request.getUsername().toLowerCase());
        
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
        
        // Validar sub-roles (solo para VENDEDOR, VENDEDOR_SIN_COMISION y ENCARGADO_LOCAL)
        String subRolError = validateSubRoles(role, request.getSubRoles());
        if (subRolError != null) {
            return ResponseEntity.badRequest().body(subRolError);
        }
        
        // Validar que los IDs de locales existan en la base de datos
        if (request.getLocales() != null && !request.getLocales().isEmpty()) {
            try {
                localService.validateLocalIds(request.getLocales());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }
        
        // Convertir sub-roles de String a enum
        List<com.muebleria.model.SubRol> subRoles = new ArrayList<>();
        if (request.getSubRoles() != null && !request.getSubRoles().isEmpty()) {
            try {
                subRoles = request.getSubRoles().stream()
                        .map(com.muebleria.model.SubRol::valueOf)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                return ResponseEntity
                        .badRequest()
                        .body("Sub-rol inválido. Valores permitidos: VENDEDOR_LOCAL, ONLINE_CON_BUSINESS, ONLINE_SIN_BUSINESS");
            }
        }
        
        // Validar locales con comisión (solo para ADMIN_LOCAL)
        if (request.getLocalesConComision() != null && !request.getLocalesConComision().isEmpty()) {
            if (role != Role.ADMIN_LOCAL) {
                return ResponseEntity
                        .badRequest()
                        .body("Solo los usuarios con rol ADMIN_LOCAL pueden tener locales con comisión");
            }
            try {
                localService.validateLocalIds(request.getLocalesConComision());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }
        
        // Crear el nuevo usuario
        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .localIds(request.getLocales() != null ? request.getLocales() : new ArrayList<>())
                .subRoles(subRoles)
                .localesConComisionIds(request.getLocalesConComision() != null ? request.getLocalesConComision() : new ArrayList<>())
                .createdBy(currentUser.getUsername())
                .active(true)
                .build();
        
        User savedUser = userRepository.save(newUser);
        
        UserResponse response = UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .locales(savedUser.getLocalIds() != null ? savedUser.getLocalIds() : new ArrayList<>())
                .subRoles(savedUser.getSubRoles() != null
                        ? savedUser.getSubRoles().stream().map(Enum::name).collect(Collectors.toList())
                        : new ArrayList<>())
                .localesConComision(savedUser.getLocalesConComisionIds() != null 
                        ? savedUser.getLocalesConComisionIds() 
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
        
        if (role == Role.ENCARGADO_LOCAL) {
            if (locales == null || locales.size() != 1) {
                return "El ENCARGADO_LOCAL debe tener exactamente 1 local asignado";
            }
        }
        
        if (role == Role.ADMIN_LOCAL || role == Role.VENDEDOR || role == Role.VENDEDOR_SIN_COMISION || role == Role.BODEGUERO) {
            if (locales == null || locales.isEmpty()) {
                return "El " + role.name() + " debe tener al menos 1 local asignado";
            }
        }
        
        return null;
    }
    
    /**
     * Validar que los sub-roles sean correctos según el rol.
     */
    private String validateSubRoles(Role role, List<String> subRoles) {
        // Sub-roles solo permitidos para VENDEDOR, VENDEDOR_SIN_COMISION y ENCARGADO_LOCAL
        if (subRoles != null && !subRoles.isEmpty()) {
            if (role != Role.VENDEDOR && role != Role.VENDEDOR_SIN_COMISION && role != Role.ENCARGADO_LOCAL) {
                return "Los sub-roles solo están permitidos para VENDEDOR, VENDEDOR_SIN_COMISION y ENCARGADO_LOCAL";
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
                        .locales(user.getLocalIds() != null 
                                ? user.getLocalIds()
                                : new ArrayList<>())
                        .subRoles(user.getSubRoles() != null
                                ? user.getSubRoles().stream().map(Enum::name).collect(Collectors.toList())
                                : new ArrayList<>())
                        .localesConComision(user.getLocalesConComisionIds() != null
                                ? user.getLocalesConComisionIds()
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
                .locales(updatedUser.getLocalIds() != null 
                        ? updatedUser.getLocalIds()
                        : new ArrayList<>())
                .subRoles(updatedUser.getSubRoles() != null
                        ? updatedUser.getSubRoles().stream().map(Enum::name).collect(Collectors.toList())
                        : new ArrayList<>())
                .localesConComision(updatedUser.getLocalesConComisionIds() != null
                        ? updatedUser.getLocalesConComisionIds()
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
            
            // Validar sub-roles si se cambia el rol
            String subRolError = validateSubRoles(newRole, request.getSubRoles());
            if (subRolError != null) {
                return ResponseEntity.badRequest().body(subRolError);
            }
        }
        
        // Actualizar locales si están presentes
        if (request.getLocales() != null) {
            try {
                localService.validateLocalIds(request.getLocales());
                user.setLocalIds(request.getLocales());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }
        
        // Actualizar sub-roles si están presentes
        if (request.getSubRoles() != null) {
            try {
                List<com.muebleria.model.SubRol> subRoles = request.getSubRoles().stream()
                        .map(com.muebleria.model.SubRol::valueOf)
                        .collect(Collectors.toList());
                user.setSubRoles(subRoles);
            } catch (IllegalArgumentException e) {
                return ResponseEntity
                        .badRequest()
                        .body("Sub-rol inválido. Valores permitidos: VENDEDOR_LOCAL, ONLINE_CON_BUSINESS, ONLINE_SIN_BUSINESS");
            }
        }
        
        // Actualizar locales con comisión si están presentes (solo para ADMIN_LOCAL)
        if (request.getLocalesConComision() != null && !request.getLocalesConComision().isEmpty()) {
            // Validar que el usuario sea ADMIN_LOCAL
            if (user.getRole() != Role.ADMIN_LOCAL) {
                return ResponseEntity
                        .badRequest()
                        .body("Solo los usuarios con rol ADMIN_LOCAL pueden tener locales con comisión");
            }
            
            try {
                localService.validateLocalIds(request.getLocalesConComision());
                user.setLocalesConComisionIds(request.getLocalesConComision());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }
        
        User updatedUser = userRepository.save(user);
        
        UserResponse response = UserResponse.builder()
                .id(updatedUser.getId())
                .username(updatedUser.getUsername())
                .email(updatedUser.getEmail())
                .role(updatedUser.getRole().name())
                .locales(updatedUser.getLocalIds() != null 
                        ? updatedUser.getLocalIds()
                        : new ArrayList<>())
                .subRoles(updatedUser.getSubRoles() != null
                        ? updatedUser.getSubRoles().stream().map(Enum::name).collect(Collectors.toList())
                        : new ArrayList<>())
                .localesConComision(updatedUser.getLocalesConComisionIds() != null
                        ? updatedUser.getLocalesConComisionIds()
                        : new ArrayList<>())
                .active(updatedUser.isActive())
                .createdAt(updatedUser.getCreatedAt())
                .createdBy(updatedUser.getCreatedBy())
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtener lista de vendedores para asignar a una venta.
     * Filtra por local si se proporciona el parámetro.
     * Accesible para ADMINISTRADOR, ADMIN_LOCAL y ENCARGADO_LOCAL.
     */
    @GetMapping("/vendedores")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_LOCAL', 'ENCARGADO_LOCAL')")
    public ResponseEntity<List<UserResponse>> getVendedores(
            @RequestParam(required = false) String local) {
        
        List<User> allUsers = userRepository.findAll();
        
        // Filtrar solo VENDEDOR, VENDEDOR_SIN_COMISION y ENCARGADO_LOCAL
        List<User> vendedores = allUsers.stream()
                .filter(u -> u.getRole() == Role.VENDEDOR || u.getRole() == Role.VENDEDOR_SIN_COMISION || u.getRole() == Role.ENCARGADO_LOCAL)
                .filter(u -> u.isActive()) // Solo usuarios activos
                .collect(Collectors.toList());
        
        // Si se especifica un local, filtrar por vendedores que tengan ese local asignado
        if (local != null && !local.isEmpty()) {
            // Validar que el local ID existe
            try {
                localService.validateActiveLocalId(local);
                vendedores = vendedores.stream()
                        .filter(u -> u.getLocalIds() != null && u.getLocalIds().contains(local))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                // Si el local es inválido o no existe, devolver lista vacía
                return ResponseEntity.ok(new ArrayList<>());
            }
        }
        
        // Convertir a UserResponse
        List<UserResponse> response = vendedores.stream()
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .locales(user.getLocalIds() != null 
                                ? user.getLocalIds()
                                : new ArrayList<>())
                        .subRoles(user.getSubRoles() != null
                                ? user.getSubRoles().stream().map(Enum::name).collect(Collectors.toList())
                                : new ArrayList<>())
                        .active(user.isActive())
                        .build())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Endpoint de migración para duplicar productos existentes por local.
     * Este endpoint toma productos con stockPorLocal y crea productos separados para cada local.
     * NOTA: Solo ejecutar UNA VEZ durante la migración.
     */
    @PostMapping("/migrate-products")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> migrateProductsByLocal() {
        return ResponseEntity.ok("Migration endpoint disabled. Use manual migration script.");
    }
}
