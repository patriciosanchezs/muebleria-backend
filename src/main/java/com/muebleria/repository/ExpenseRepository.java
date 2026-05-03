package com.muebleria.repository;

import com.muebleria.model.CategoriaGasto;
import com.muebleria.model.Expense;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseRepository extends MongoRepository<Expense, String> {

    List<Expense> findByLocalId(String localId);

    List<Expense> findByLocalIdIn(List<String> localIds);

    List<Expense> findByCategoria(CategoriaGasto categoria);

    List<Expense> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

    List<Expense> findByLocalIdAndFechaBetween(String localId, LocalDateTime inicio, LocalDateTime fin);

    List<Expense> findByLocalIdInAndFechaBetween(List<String> localIds, LocalDateTime inicio, LocalDateTime fin);

    List<Expense> findByLocalIdInAndCategoria(List<String> localIds, CategoriaGasto categoria);

    List<Expense> findByLocalIdInAndCategoriaAndFechaBetween(List<String> localIds, CategoriaGasto categoria, LocalDateTime inicio, LocalDateTime fin);

    List<Expense> findByMetodoPago(String metodoPago);

    List<Expense> findByLocalIdInAndMetodoPago(List<String> localIds, String metodoPago);

    List<Expense> findAllByOrderByFechaDesc();

    List<Expense> findByLocalIdInOrderByFechaDesc(List<String> localIds);
}
