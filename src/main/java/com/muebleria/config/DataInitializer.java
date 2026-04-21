package com.muebleria.config;

import com.muebleria.model.Role;
import com.muebleria.model.User;
import com.muebleria.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Inicializa la base de datos con un usuario ADMINISTRADOR por defecto.
 * Solo se ejecuta si no existen usuarios ADMINISTRADOR en el sistema.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        // Verificar si ya existe algún administrador
        long adminCount = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ADMINISTRADOR)
                .count();
        
        if (adminCount == 0) {
            log.info("No se encontraron administradores. Creando usuario administrador por defecto...");
            
            User admin = User.builder()
                    .username("admin")
                    .email("admin@muebleria.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMINISTRADOR)
                    .localIds(new ArrayList<>()) // Admin no tiene locales específicos (acceso total)
                    .active(true)
                    .createdBy("SYSTEM")
                    .build();
            
            userRepository.save(admin);
            
            log.info("===================================================");
            log.info("Usuario ADMINISTRADOR creado exitosamente");
            log.info("Username: admin");
            log.info("Password: admin123");
            log.info("¡CAMBIA LA CONTRASEÑA INMEDIATAMENTE!");
            log.info("===================================================");
        } else {
            log.info("Ya existen {} administrador(es) en el sistema", adminCount);
        }
    }
}
