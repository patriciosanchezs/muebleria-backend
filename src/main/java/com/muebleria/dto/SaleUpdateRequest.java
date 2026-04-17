package com.muebleria.dto;

import com.muebleria.model.SaleItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleUpdateRequest {
    
    @NotEmpty(message = "La venta debe tener al menos un producto")
    private List<SaleItemRequest> items;
    
    @NotBlank(message = "El método de pago es obligatorio")
    private String metodoPago;
    
    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String clienteNombre;
    
    @NotBlank(message = "La dirección del cliente es obligatoria")
    private String clienteDireccion;
    
    @NotBlank(message = "El correo del cliente es obligatorio")
    private String clienteCorreo;
    
    @NotBlank(message = "El teléfono del cliente es obligatorio")
    private String clienteTelefono;
    
    private LocalDateTime fechaDespacho;
    
    private String notas;
}
