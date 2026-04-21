package com.muebleria.service;

import com.muebleria.model.Role;
import com.muebleria.model.User;
import com.muebleria.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuthHelper {
    
    private final UserRepository userRepository;
    private final LocalService localService;
    
    /**
     * Obtiene el usuario autenticado
     */
    public User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username).orElse(null);
    }
    
    /**
     * Obtiene los IDs de locales autorizados del usuario
     * - ADMINISTRADOR: Retorna todos los locales activos de la BD
     * - ADMIN_LOCAL: Retorna solo sus locales asignados
     * - Otros roles: Retorna sus locales asignados
     */
    public List<String> getAuthorizedLocales(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        
        if (user == null) {
            return List.of();
        }
        
        // ADMINISTRADOR tiene acceso a todos los locales
        if (user.getRole() == Role.ADMINISTRADOR) {
            return localService.getAllActiveLocalEntities().stream()
                    .map(local -> local.getId())
                    .collect(Collectors.toList());
        }
        
        // ADMIN_LOCAL y otros roles: solo sus locales asignados
        return user.getLocalIds() != null ? user.getLocalIds() : List.of();
    }
    
    /**
     * Verifica si el usuario es administrador global
     */
    public boolean isGlobalAdmin(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return user != null && user.getRole() == Role.ADMINISTRADOR;
    }
    
    /**
     * Verifica si el usuario es admin local
     */
    public boolean isAdminLocal(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return user != null && user.getRole() == Role.ADMIN_LOCAL;
    }
    
    /**
     * Verifica si el usuario es administrador (alias de isGlobalAdmin)
     */
    public boolean isAdmin(Authentication authentication) {
        return isGlobalAdmin(authentication);
    }
    
    /**
     * Obtiene los locales del usuario (alias de getAuthorizedLocales)
     */
    public List<String> getUserLocales(Authentication authentication) {
        return getAuthorizedLocales(authentication);
    }
}
