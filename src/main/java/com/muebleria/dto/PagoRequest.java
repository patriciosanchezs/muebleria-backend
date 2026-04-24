package com.muebleria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PagoRequest {
    
    @NotBlank(message = "La forma de pago es requerida")
    @Pattern(
        regexp = "EFECTIVO|TRANSFERENCIA|DEBITO|CREDITO",
        message = "Forma de pago debe ser: EFECTIVO, TRANSFERENCIA, DEBITO o CREDITO"
    )
    private String formaDePago;
    
    @NotNull(message = "El monto es requerido")
    @Positive(message = "El monto debe ser mayor a 0")
    private Double monto;
}
