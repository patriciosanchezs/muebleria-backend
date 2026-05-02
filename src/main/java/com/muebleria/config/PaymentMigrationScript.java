package com.muebleria.config;

import com.muebleria.model.Payment;
import com.muebleria.model.Sale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.List;

/**
 * Migración para convertir Sale.metodoPago (String) a Sale.payments (List<Payment>)
 * Se ejecuta una sola vez al iniciar la aplicación.
 */
@Configuration
public class PaymentMigrationScript {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Bean
    @Order(3) // Después de LocalDataInitializer (1) y DataMigrationScript (2)
    public CommandLineRunner migratePayments() {
        return args -> {
            System.out.println("=== Iniciando migración de pagos ===");
            
            try {
                // Buscar ventas que tienen metodoPago pero no tienen payments
                Query query = new Query(Criteria.where("metodoPago").exists(true)
                        .and("payments").exists(false));
                
                List<Sale> salesWithOldFormat = mongoTemplate.find(query, Sale.class);
                
                if (salesWithOldFormat.isEmpty()) {
                    System.out.println("No hay ventas que migrar. Todas las ventas ya tienen el nuevo formato de pagos.");
                    return;
                }
                
                System.out.println("Encontradas " + salesWithOldFormat.size() + " ventas para migrar...");
                
                int migratedCount = 0;
                int errorCount = 0;
                
                for (Sale sale : salesWithOldFormat) {
                    try {
                        // Obtener el método de pago antiguo
                        String metodoPago = getMetodoPagoFromDocument(sale.getId());
                        
                        if (metodoPago != null && !metodoPago.isEmpty()) {
                            // Crear un pago único con el total de la venta
                            List<Payment> payments = new ArrayList<>();
                            Payment payment = Payment.builder()
                                    .paymentMethod(metodoPago)
                                    .amount(sale.getTotalCLP())
                                    .paymentDate(sale.getFechaVenta())
                                    .build();
                            payments.add(payment);
                            
                            // Actualizar la venta
                            Query updateQuery = new Query(Criteria.where("_id").is(sale.getId()));
                            Update update = new Update()
                                    .set("payments", payments)
                                    .unset("metodoPago"); // Eliminar el campo antiguo
                            
                            mongoTemplate.updateFirst(updateQuery, update, Sale.class);
                            migratedCount++;
                            
                            if (migratedCount % 100 == 0) {
                                System.out.println("Migradas " + migratedCount + " ventas...");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error migrando venta " + sale.getId() + ": " + e.getMessage());
                        errorCount++;
                    }
                }
                
                System.out.println("=== Migración de pagos completada ===");
                System.out.println("Ventas migradas exitosamente: " + migratedCount);
                if (errorCount > 0) {
                    System.out.println("Ventas con errores: " + errorCount);
                }
                
            } catch (Exception e) {
                System.err.println("Error durante la migración de pagos: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
    
    /**
     * Obtiene el metodoPago directamente del documento MongoDB
     */
    private String getMetodoPagoFromDocument(String saleId) {
        try {
            Query query = new Query(Criteria.where("_id").is(saleId));
            org.bson.Document doc = mongoTemplate.getCollection("sales").find(query.getQueryObject()).first();
            if (doc != null && doc.containsKey("metodoPago")) {
                return doc.getString("metodoPago");
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo metodoPago para venta " + saleId + ": " + e.getMessage());
        }
        return null;
    }
}
