package com.muebleria.config;

import com.muebleria.model.LocalEntity;
import com.muebleria.model.Product;
import com.muebleria.model.Sale;
import com.muebleria.model.Commission;
import com.muebleria.model.User;
import com.muebleria.repository.LocalRepository;
import com.muebleria.repository.ProductRepository;
import com.muebleria.repository.SaleRepository;
import com.muebleria.repository.CommissionRepository;
import com.muebleria.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.List;

/**
 * Script de migración de datos para convertir códigos de locales antiguos
 * (QUILLOTA, COQUIMBO, MUEBLES_SANCHEZ) a IDs de MongoDB
 */
@Component
@Order(2) // Ejecutar después de LocalDataInitializer
@RequiredArgsConstructor
@Slf4j
public class DataMigrationScript implements CommandLineRunner {
    
    private final LocalRepository localRepository;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final CommissionRepository commissionRepository;
    private final UserRepository userRepository;
    
    @Override
    public void run(String... args) {
        log.info("=== Iniciando migración de datos de locales ===");
        
        // Crear mapa de códigos antiguos a IDs nuevos
        Map<String, String> codigoToIdMap = new HashMap<>();
        
        // Buscar los locales por código
        Optional<LocalEntity> quillota = localRepository.findByCodigo("QUILLOTA");
        Optional<LocalEntity> coquimbo = localRepository.findByCodigo("COQUIMBO");
        Optional<LocalEntity> mueblesSanchez = localRepository.findByCodigo("MUEBLES_SANCHEZ");
        
        if (quillota.isEmpty() || coquimbo.isEmpty() || mueblesSanchez.isEmpty()) {
            log.warn("No se encontraron todos los locales necesarios para la migración");
            log.warn("Quillota: {}, Coquimbo: {}, Muebles Sánchez: {}", 
                quillota.isPresent(), coquimbo.isPresent(), mueblesSanchez.isPresent());
            return;
        }
        
        codigoToIdMap.put("QUILLOTA", quillota.get().getId());
        codigoToIdMap.put("COQUIMBO", coquimbo.get().getId());
        codigoToIdMap.put("MUEBLES_SANCHEZ", mueblesSanchez.get().getId());
        
        log.info("Mapa de migración creado:");
        codigoToIdMap.forEach((codigo, id) -> log.info("  {} -> {}", codigo, id));
        
        // Migrar usuarios
        migrateUsers(codigoToIdMap);
        
        // Migrar productos
        migrateProducts(codigoToIdMap);
        
        // Migrar ventas
        migrateSales(codigoToIdMap);
        
        // Migrar comisiones
        migrateCommissions(codigoToIdMap);
        
        log.info("=== Migración de datos completada ===");
    }
    
    private void migrateUsers(Map<String, String> codigoToIdMap) {
        log.info("Migrando usuarios...");
        int migratedUsers = 0;
        int migratedLocalIds = 0;
        int migratedComisionIds = 0;
        
        for (User user : userRepository.findAll()) {
            boolean userModified = false;
            
            // Migrar localIds
            if (user.getLocalIds() != null && !user.getLocalIds().isEmpty()) {
                List<String> newLocalIds = user.getLocalIds().stream()
                    .map(oldId -> codigoToIdMap.getOrDefault(oldId, oldId))
                    .collect(Collectors.toList());
                
                if (!newLocalIds.equals(user.getLocalIds())) {
                    user.setLocalIds(newLocalIds);
                    userModified = true;
                    migratedLocalIds++;
                    log.debug("Usuario {} localIds migrados: {} -> {}", 
                        user.getUsername(), user.getLocalIds(), newLocalIds);
                }
            }
            
            // Migrar localesConComisionIds
            if (user.getLocalesConComisionIds() != null && !user.getLocalesConComisionIds().isEmpty()) {
                List<String> newComisionIds = user.getLocalesConComisionIds().stream()
                    .map(oldId -> codigoToIdMap.getOrDefault(oldId, oldId))
                    .collect(Collectors.toList());
                
                if (!newComisionIds.equals(user.getLocalesConComisionIds())) {
                    user.setLocalesConComisionIds(newComisionIds);
                    userModified = true;
                    migratedComisionIds++;
                    log.debug("Usuario {} localesConComisionIds migrados: {} -> {}", 
                        user.getUsername(), user.getLocalesConComisionIds(), newComisionIds);
                }
            }
            
            if (userModified) {
                userRepository.save(user);
                migratedUsers++;
            }
        }
        
        log.info("Usuarios migrados: {} (localIds: {}, localesConComisionIds: {})", 
            migratedUsers, migratedLocalIds, migratedComisionIds);
    }
    
    private void migrateProducts(Map<String, String> codigoToIdMap) {
        log.info("Migrando productos...");
        int migrated = 0;
        
        for (Product product : productRepository.findAll()) {
            String oldLocalId = product.getLocalId();
            if (codigoToIdMap.containsKey(oldLocalId)) {
                String newLocalId = codigoToIdMap.get(oldLocalId);
                product.setLocalId(newLocalId);
                productRepository.save(product);
                migrated++;
                log.debug("Producto {} migrado de {} a {}", product.getId(), oldLocalId, newLocalId);
            }
        }
        
        log.info("Productos migrados: {}", migrated);
    }
    
    private void migrateSales(Map<String, String> codigoToIdMap) {
        log.info("Migrando ventas...");
        int migrated = 0;
        
        for (Sale sale : saleRepository.findAll()) {
            String oldLocalId = sale.getLocalId();
            if (codigoToIdMap.containsKey(oldLocalId)) {
                String newLocalId = codigoToIdMap.get(oldLocalId);
                sale.setLocalId(newLocalId);
                saleRepository.save(sale);
                migrated++;
                log.debug("Venta {} migrada de {} a {}", sale.getId(), oldLocalId, newLocalId);
            }
        }
        
        log.info("Ventas migradas: {}", migrated);
    }
    
    private void migrateCommissions(Map<String, String> codigoToIdMap) {
        log.info("Migrando comisiones...");
        int migrated = 0;
        
        for (Commission commission : commissionRepository.findAll()) {
            String oldLocalId = commission.getLocalId();
            if (codigoToIdMap.containsKey(oldLocalId)) {
                String newLocalId = codigoToIdMap.get(oldLocalId);
                commission.setLocalId(newLocalId);
                commissionRepository.save(commission);
                migrated++;
                log.debug("Comisión {} migrada de {} a {}", commission.getId(), oldLocalId, newLocalId);
            }
        }
        
        log.info("Comisiones migradas: {}", migrated);
    }
}
