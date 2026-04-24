package com.muebleria.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pago {
    
    // Método de pago: EFECTIVO, TRANSFERENCIA, DEBITO, CREDITO
    private String formaDePago;
    
    // Monto pagado con este método (en CLP)
    private Double monto;
}
