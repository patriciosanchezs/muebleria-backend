package com.muebleria.repository;

import com.muebleria.model.Commission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommissionRepository extends MongoRepository<Commission, String> {
    List<Commission> findByVendedorUsername(String vendedorUsername);
    List<Commission> findByVendedorUsernameAndFechaVentaBetween(String vendedorUsername, LocalDateTime start, LocalDateTime end);
    List<Commission> findByFechaVentaBetween(LocalDateTime start, LocalDateTime end);
}
