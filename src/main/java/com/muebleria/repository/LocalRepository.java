package com.muebleria.repository;

import com.muebleria.model.LocalEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la gestión de locales en MongoDB
 */
@Repository
public interface LocalRepository extends MongoRepository<LocalEntity, String> {
    
    /**
     * Buscar un local por su código único
     * @param codigo Código del local (ej: QUILLOTA)
     * @return Optional con el local si existe
     */
    Optional<LocalEntity> findByCodigo(String codigo);
    
    /**
     * Verificar si existe un local con un código específico
     * @param codigo Código a verificar
     * @return true si existe
     */
    boolean existsByCodigo(String codigo);
    
    /**
     * Obtener todos los locales activos
     * @return Lista de locales activos
     */
    List<LocalEntity> findByActivoTrue();
    
    /**
     * Obtener todos los locales ordenados por nombre
     * @return Lista de todos los locales
     */
    @Query(sort = "{ 'nombre' : 1 }")
    List<LocalEntity> findAllByOrderByNombreAsc();
    
    /**
     * Buscar locales por ciudad
     * @param ciudad Ciudad a buscar
     * @return Lista de locales en esa ciudad
     */
    List<LocalEntity> findByCiudad(String ciudad);
}
