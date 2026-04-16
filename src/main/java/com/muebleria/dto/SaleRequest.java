package com.muebleria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class SaleRequest {
    
    @NotEmpty(message = "Debe incluir al menos un producto")
    @Valid
    private List<SaleItemRequest> items;
    
    @NotBlank(message = "El método de pago es requerido")
    @Pattern(
        regexp = "EFECTIVO|TRANSFERENCIA|DEBITO|CREDITO",
        message = "Método de pago debe ser: EFECTIVO, TRANSFERENCIA, DEBITO o CREDITO"
    )
    private String metodoPago;
    
    // Datos del cliente
    @NotBlank(message = "El nombre del cliente es requerido")
    private String clienteNombre;
    
    @NotBlank(message = "La dirección del cliente es requerida")
    private String clienteDireccion;
    
    @Email(message = "El correo del cliente debe ser válido")
    @NotBlank(message = "El correo del cliente es requerido")
    private String clienteCorreo;
    
    @NotBlank(message = "El teléfono del cliente es requerido")
    @Pattern(
        regexp = "^\\+?[0-9]{8,15}$",
        message = "El teléfono debe tener entre 8 y 15 dígitos"
    )
    private String clienteTelefono;
    
    // Gestión de entregas
    @NotBlank(message = "El tipo de entrega es requerido")
    @Pattern(
        regexp = "ENTREGADO|DESPACHO",
        message = "Tipo de entrega debe ser: ENTREGADO o DESPACHO"
    )
    private String tipoEntrega;
    
    // Fecha programada para el despacho (solo si tipoEntrega = DESPACHO)
    // Formato: ISO 8601 (yyyy-MM-dd'T'HH:mm:ss)
    private String fechaDespacho;
    
    // Local/Sucursal donde se realiza la venta
    @NotBlank(message = "El local es requerido")
    @Pattern(
        regexp = "QUILLOTA|COQUIMBO|MUEBLES_SANCHEZ",
        message = "Local debe ser: QUILLOTA, COQUIMBO o MUEBLES_SANCHEZ"
    )
    private String local;
    
    // Canal de venta
    @NotBlank(message = "El canal de venta es requerido")
    @Pattern(
        regexp = "EN_LOCAL|ONLINE_BUSINESS|ONLINE_SIN_BUSINESS",
        message = "Canal de venta debe ser: EN_LOCAL, ONLINE_BUSINESS o ONLINE_SIN_BUSINESS"
    )
    private String canalVenta;
    
    private String notas;
}
