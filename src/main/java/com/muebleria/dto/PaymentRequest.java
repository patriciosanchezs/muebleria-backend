package com.muebleria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentRequest {

    @NotBlank(message = "Payment method is required")
    @Pattern(
        regexp = "EFECTIVO|TRANSFERENCIA|DEBITO|CREDITO",
        message = "Payment method must be: EFECTIVO, TRANSFERENCIA, DEBITO or CREDITO"
    )
    private String paymentMethod;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    private Double amount;

    private LocalDateTime paymentDate;
}
