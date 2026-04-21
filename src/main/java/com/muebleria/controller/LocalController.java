package com.muebleria.controller;

import com.muebleria.dto.LocalRequest;
import com.muebleria.dto.LocalResponse;
import com.muebleria.service.LocalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para la gestión de locales
 */
@RestController
@RequestMapping("/locales")
@RequiredArgsConstructor
public class LocalController {
    
    private final LocalService localService;
    
    /**
     * Obtener todos los locales activos
     * Accesible por todos los roles autenticados
     */
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LocalResponse>> getActiveLocales() {
        return ResponseEntity.ok(localService.getAllActiveLocales());
    }
    
    /**
     * Obtener los locales asignados al usuario autenticado
     * Si es ADMINISTRADOR, devuelve todos los locales activos
     * Si tiene locales asignados, devuelve solo esos
     */
    @GetMapping("/my-locales")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LocalResponse>> getMyLocales(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(localService.getLocalesByUsername(username));
    }
    
    /**
     * Obtener todos los locales (activos e inactivos)
     * Solo ADMINISTRADOR puede ver locales inactivos
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<LocalResponse>> getAllLocales() {
        return ResponseEntity.ok(localService.getAllLocales());
    }
    
    /**
     * Obtener un local por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LocalResponse> getLocalById(@PathVariable String id) {
        return ResponseEntity.ok(localService.getLocalById(id));
    }
    
    /**
     * Obtener un local por código
     */
    @GetMapping("/codigo/{codigo}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LocalResponse> getLocalByCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(localService.getLocalByCodigo(codigo));
    }
    
    /**
     * Crear un nuevo local
     * Solo ADMINISTRADOR puede crear locales
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<LocalResponse> createLocal(
            @Valid @RequestBody LocalRequest request,
            Authentication authentication) {
        
        String createdBy = authentication != null ? authentication.getName() : "Sistema";
        LocalResponse response = localService.createLocal(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Actualizar un local existente
     * Solo ADMINISTRADOR puede actualizar locales
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<LocalResponse> updateLocal(
            @PathVariable String id,
            @Valid @RequestBody LocalRequest request,
            Authentication authentication) {
        
        String updatedBy = authentication != null ? authentication.getName() : "Sistema";
        LocalResponse response = localService.updateLocal(id, request, updatedBy);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Activar/desactivar un local
     * Solo ADMINISTRADOR puede cambiar el estado
     */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<LocalResponse> toggleLocalStatus(
            @PathVariable String id,
            Authentication authentication) {
        
        String updatedBy = authentication != null ? authentication.getName() : "Sistema";
        LocalResponse response = localService.toggleLocalStatus(id, updatedBy);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Eliminar (desactivar) un local
     * Solo ADMINISTRADOR puede eliminar locales
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<String> deleteLocal(
            @PathVariable String id,
            Authentication authentication) {
        
        String updatedBy = authentication != null ? authentication.getName() : "Sistema";
        localService.deleteLocal(id, updatedBy);
        return ResponseEntity.ok("Local desactivado exitosamente");
    }
}
