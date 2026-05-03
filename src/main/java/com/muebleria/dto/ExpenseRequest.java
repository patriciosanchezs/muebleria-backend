package com.muebleria.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExpenseRequest {

    @NotBlank(message = "El local es obligatorio")
    private String localId;

    @NotNull(message = "La categoría es obligatoria")
    private String categoria;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    @NotNull(message = "El monto es obligatorio")
    @Min(value = 1, message = "El monto debe ser mayor a 0")
    private Double monto;

    private LocalDateTime fecha;

    @NotBlank(message = "El método de pago es obligatorio")
    private String metodoPago;

    private String comprobante;

    private String notas;
}
