package com.muebleria.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    private String paymentMethod;

    private Double amount;

    @Builder.Default
    private LocalDateTime paymentDate = LocalDateTime.now();
}
