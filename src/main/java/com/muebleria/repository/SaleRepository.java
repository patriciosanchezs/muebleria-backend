package com.muebleria.repository;

import com.muebleria.model.Sale;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends MongoRepository<Sale, String> {
    
    List<Sale> findByFechaVentaBetween(LocalDateTime start, LocalDateTime end);
    
    List<Sale> findByMetodoPago(String metodoPago);
    
    List<Sale> findByVendedor(String vendedor);
    
    List<Sale> findByVendedorAndFechaVentaBetween(String vendedor, LocalDateTime start, LocalDateTime end);
    
    // Consultas para gestión de entregas
    List<Sale> findByEstadoEntrega(String estadoEntrega);
    
    List<Sale> findByTipoEntregaAndEstadoEntrega(String tipoEntrega, String estadoEntrega);
    
    List<Sale> findByFechaDespachoBetween(LocalDateTime start, LocalDateTime end);
    
    List<Sale> findByEstadoEntregaAndFechaDespachoBetween(String estadoEntrega, LocalDateTime start, LocalDateTime end);
}
