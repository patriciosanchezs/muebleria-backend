package com.muebleria.repository;

import com.muebleria.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    
    List<Product> findByCategoria(String categoria);
    
    List<Product> findByDisponible(boolean disponible);
    
    List<Product> findByNombreContainingIgnoreCase(String nombre);
    
    @Query("{ 'stockReservado': { $gt: 0 }, 'reservaTimestamp': { $lt: ?0 } }")
    List<Product> findExpiredReservations(LocalDateTime before);
}
