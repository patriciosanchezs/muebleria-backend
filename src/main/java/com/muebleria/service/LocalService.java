package com.muebleria.service;

import com.muebleria.dto.LocalRequest;
import com.muebleria.dto.LocalResponse;
import com.muebleria.exception.BadRequestException;
import com.muebleria.exception.ResourceNotFoundException;
import com.muebleria.model.LocalEntity;
import com.muebleria.model.Role;
import com.muebleria.model.User;
import com.muebleria.repository.LocalRepository;
import com.muebleria.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de locales
 */
@Service
@RequiredArgsConstructor
public class LocalService {
    
    private final LocalRepository localRepository;
    private final UserRepository userRepository;
    
    /**
     * Obtener todos los locales activos
     */
    public List<LocalResponse> getAllActiveLocales() {
        return localRepository.findByActivoTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtener todos los locales (activos e inactivos)
     */
    public List<LocalResponse> getAllLocales() {
        return localRepository.findAllByOrderByNombreAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtener los locales asignados a un usuario específico
     * Si el usuario es ADMINISTRADOR, devuelve todos los locales activos
     * Si el usuario tiene locales asignados, devuelve solo esos
     */
    public List<LocalResponse> getLocalesByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
        
        // Si es ADMINISTRADOR, devolver todos los locales activos
        if (user.getRole() == Role.ADMINISTRADOR) {
            return getAllActiveLocales();
        }
        
        // Si no tiene locales asignados, devolver lista vacía
        if (user.getLocalIds() == null || user.getLocalIds().isEmpty()) {
            return new ArrayList<>();
        }
        
        // Obtener solo los locales asignados al usuario que estén activos
        return user.getLocalIds().stream()
                .map(localId -> localRepository.findById(localId).orElse(null))
                .filter(local -> local != null && local.isActivo())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtener un local por ID
     */
    public LocalResponse getLocalById(String id) {
        LocalEntity local = localRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con ID: " + id));
        return toResponse(local);
    }
    
    /**
     * Obtener un local por código
     */
    public LocalResponse getLocalByCodigo(String codigo) {
        LocalEntity local = localRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con código: " + codigo));
        return toResponse(local);
    }
    
    /**
     * Obtener entidad local por ID (para uso interno)
     */
    public LocalEntity getLocalEntityById(String id) {
        return localRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con ID: " + id));
    }
    
    /**
     * Obtener entidad local por código (para uso interno)
     */
    public LocalEntity getLocalEntityByCodigo(String codigo) {
        return localRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con código: " + codigo));
    }
    
    /**
     * Obtener todas las entidades de locales activos (para uso interno)
     */
    public List<LocalEntity> getAllActiveLocalEntities() {
        return localRepository.findByActivoTrue();
    }
    
    /**
     * Crear un nuevo local
     */
    @Transactional
    public LocalResponse createLocal(LocalRequest request, String createdBy) {
        // Validar que el código no exista
        if (localRepository.existsByCodigo(request.getCodigo())) {
            throw new BadRequestException("Ya existe un local con el código: " + request.getCodigo());
        }
        
        LocalEntity local = LocalEntity.builder()
                .codigo(request.getCodigo().toUpperCase())
                .nombre(request.getNombre())
                .direccion(request.getDireccion())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .ciudad(request.getCiudad())
                .region(request.getRegion())
                .activo(true)
                .createdAt(LocalDateTime.now())
                .createdBy(createdBy)
                .build();
        
        LocalEntity savedLocal = localRepository.save(local);
        return toResponse(savedLocal);
    }
    
    /**
     * Actualizar un local existente
     */
    @Transactional
    public LocalResponse updateLocal(String id, LocalRequest request, String updatedBy) {
        LocalEntity local = localRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con ID: " + id));
        
        // Si se cambia el código, validar que no exista otro local con ese código
        if (!local.getCodigo().equals(request.getCodigo())) {
            if (localRepository.existsByCodigo(request.getCodigo())) {
                throw new BadRequestException("Ya existe otro local con el código: " + request.getCodigo());
            }
            local.setCodigo(request.getCodigo().toUpperCase());
        }
        
        local.setNombre(request.getNombre());
        local.setDireccion(request.getDireccion());
        local.setTelefono(request.getTelefono());
        local.setEmail(request.getEmail());
        local.setCiudad(request.getCiudad());
        local.setRegion(request.getRegion());
        local.setUpdatedAt(LocalDateTime.now());
        local.setUpdatedBy(updatedBy);
        
        LocalEntity updatedLocal = localRepository.save(local);
        return toResponse(updatedLocal);
    }
    
    /**
     * Activar o desactivar un local
     */
    @Transactional
    public LocalResponse toggleLocalStatus(String id, String updatedBy) {
        LocalEntity local = localRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con ID: " + id));
        
        local.setActivo(!local.isActivo());
        local.setUpdatedAt(LocalDateTime.now());
        local.setUpdatedBy(updatedBy);
        
        LocalEntity updatedLocal = localRepository.save(local);
        return toResponse(updatedLocal);
    }
    
    /**
     * Eliminar un local (eliminación lógica - desactivar)
     */
    @Transactional
    public void deleteLocal(String id, String updatedBy) {
        LocalEntity local = localRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con ID: " + id));
        
        // En lugar de eliminar, desactivamos el local
        local.setActivo(false);
        local.setUpdatedAt(LocalDateTime.now());
        local.setUpdatedBy(updatedBy);
        localRepository.save(local);
    }
    
    /**
     * Validar que una lista de IDs de locales existan
     */
    public void validateLocalIds(List<String> localIds) {
        if (localIds == null || localIds.isEmpty()) {
            return;
        }
        
        for (String localId : localIds) {
            if (!localRepository.existsById(localId)) {
                throw new BadRequestException("Local no encontrado con ID: " + localId);
            }
        }
    }
    
    /**
     * Validar que un ID de local exista y esté activo
     */
    public void validateActiveLocalId(String localId) {
        LocalEntity local = localRepository.findById(localId)
                .orElseThrow(() -> new BadRequestException("Local no encontrado con ID: " + localId));
        
        if (!local.isActivo()) {
            throw new BadRequestException("El local '" + local.getNombre() + "' no está activo");
        }
    }
    
    /**
     * Convertir entidad a DTO de respuesta
     */
    private LocalResponse toResponse(LocalEntity local) {
        return LocalResponse.builder()
                .id(local.getId())
                .codigo(local.getCodigo())
                .nombre(local.getNombre())
                .direccion(local.getDireccion())
                .telefono(local.getTelefono())
                .email(local.getEmail())
                .ciudad(local.getCiudad())
                .region(local.getRegion())
                .activo(local.isActivo())
                .createdAt(local.getCreatedAt())
                .createdBy(local.getCreatedBy())
                .updatedAt(local.getUpdatedAt())
                .updatedBy(local.getUpdatedBy())
                .build();
    }
}
