package com.muebleria.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationCleanupTask {
    
    private static final Logger log = LoggerFactory.getLogger(ReservationCleanupTask.class);
    
    private final ProductService productService;
    
    @Scheduled(cron = "0 */10 * * * *")
    public void cleanupExpiredReservations() {
        log.info("Iniciando limpieza de reservas expiradas...");
        productService.limpiarReservasExpiradas();
        log.info("Limpieza de reservas expiradas completada.");
    }
}