package com.muebleria.config;

import com.muebleria.model.LocalEntity;
import com.muebleria.repository.LocalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Inicializador de datos de locales
 * Se ejecuta al iniciar la aplicación y crea los locales por defecto si no existen
 */
@Component
@Order(1) // Se ejecuta primero
@RequiredArgsConstructor
@Slf4j
public class LocalDataInitializer implements CommandLineRunner {
    
    private final LocalRepository localRepository;
    
    @Override
    public void run(String... args) {
        log.info("Inicializando datos de locales...");
        
        // Crear locales por defecto si no existen
        createLocalIfNotExists(
            "QUILLOTA",
            "Local Quillota",
            "Av. Principal #123, Quillota",
            "+56 9 1234 5678",
            "[email protected]",
            "Quillota",
            "Valparaíso"
        );
        
        createLocalIfNotExists(
            "COQUIMBO",
            "Local Coquimbo",
            "Av. Costanera #456, Coquimbo",
            "+56 9 8765 4321",
            "[email protected]",
            "Coquimbo",
            "Coquimbo"
        );
        
        createLocalIfNotExists(
            "MUEBLES_SANCHEZ",
            "Muebles Sánchez",
            "Calle Comercio #789",
            "+56 9 5555 6666",
            "[email protected]",
            "Santiago",
            "Metropolitana"
        );
        
        log.info("Inicialización de locales completada. Total de locales: {}", localRepository.count());
    }
    
    private void createLocalIfNotExists(
            String codigo,
            String nombre,
            String direccion,
            String telefono,
            String email,
            String ciudad,
            String region) {
        
        if (!localRepository.existsByCodigo(codigo)) {
            LocalEntity local = LocalEntity.builder()
                    .codigo(codigo)
                    .nombre(nombre)
                    .direccion(direccion)
                    .telefono(telefono)
                    .email(email)
                    .ciudad(ciudad)
                    .region(region)
                    .activo(true)
                    .createdAt(LocalDateTime.now())
                    .createdBy("Sistema")
                    .build();
            
            localRepository.save(local);
            log.info("Local creado: {} - {}", codigo, nombre);
        } else {
            log.info("Local ya existe: {}", codigo);
        }
    }
}
