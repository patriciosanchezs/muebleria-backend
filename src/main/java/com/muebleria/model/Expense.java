package com.muebleria.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "expenses")
public class Expense {

    @Id
    private String id;

    // Local/Sucursal al que pertenece el gasto (ID de LocalEntity)
    private String localId;

    // Categoría del gasto
    private CategoriaGasto categoria;

    // Descripción detallada del gasto
    private String descripcion;

    // Monto en pesos chilenos (CLP)
    private Double monto;

    // Fecha del gasto
    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();

    // Método de pago: EFECTIVO, TRANSFERENCIA, DEBITO, CREDITO
    private String metodoPago;

    // Número de comprobante/factura/recibo (opcional)
    private String comprobante;

    // Notas adicionales (opcional)
    private String notas;

    // Username de quien registró el gasto
    private String registradoPor;

    // Auditoría
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
