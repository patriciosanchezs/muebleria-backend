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
    
    @NotEmpty(message = "Debe incluir al menos un método de pago")
    @Valid
    private List<PagoRequest> pagos;
    
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
    // Formato: ISO 8601 (yyyy-MM-dd)
    private String fechaDespacho;

    // Monto del flete (opcional, puede ser 0)
    // Requerido cuando tipoEntrega = DESPACHO
    private Double montoFlete;

    // Local/Sucursal donde se realiza la venta (ID de LocalEntity)
    @NotBlank(message = "El local es requerido")
    private String local;
    
    // Canal de venta
    @NotBlank(message = "El canal de venta es requerido")
    @Pattern(
        regexp = "EN_LOCAL|ONLINE_BUSINESS|ONLINE_SIN_BUSINESS",
        message = "Canal de venta debe ser: EN_LOCAL, ONLINE_BUSINESS o ONLINE_SIN_BUSINESS"
    )
    private String canalVenta;
    
    // Vendedor asignado (opcional) - solo para roles administrativos
    // Si se asigna, este vendedor recibirá la comisión
    private String vendedorAsignado;
    
    private String notas;
}
