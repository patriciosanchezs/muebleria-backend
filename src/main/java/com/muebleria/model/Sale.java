package com.muebleria.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "sales")
public class Sale {
    
    @Id
    private String id;
    
    private List<SaleItem> items;
    
    // Total en pesos chilenos (CLP)
    private Double totalCLP;
    
    // Métodos de pago utilizados (puede ser uno o varios)
    private List<Payment> payments;
    
    @Builder.Default
    private LocalDateTime fechaVenta = LocalDateTime.now();
    
    // Usuario que realizó la venta (vendedor)
    private String vendedor;
    
    // Datos del cliente
    private String clienteNombre;
    
    private String clienteDireccion;
    
    private String clienteCorreo;
    
    private String clienteTelefono;
    
    // Gestión de entregas
    // Tipo de entrega: ENTREGADO (retirado en tienda) o DESPACHO (envío a domicilio)
    private String tipoEntrega;
    
    // Estado de entrega: ENTREGADO o POR_ENTREGAR
    @Builder.Default
    private String estadoEntrega = "POR_ENTREGAR";
    
    // Fecha programada para el despacho (solo si tipoEntrega = DESPACHO)
    private LocalDateTime fechaDespacho;
    
    // Usuario que entregó el pedido (fletero) - solo si estadoEntrega = ENTREGADO
    private String entregadoPor;
    
    // Fecha en que se entregó el pedido
    private LocalDateTime fechaEntrega;
    
    // Información del flete (solo si tipoEntrega = DESPACHO y estadoEntrega = ENTREGADO)
    private Double montoFlete;
    
    // Método de pago del flete: EFECTIVO, TRANSFERENCIA, DEBITO, CREDITO
    private String metodoPagoFlete;
    
    // Local/Sucursal donde se realizó la venta (ID de LocalEntity)
    private String localId;
    
    // Canal de venta: EN_LOCAL, ONLINE_BUSINESS, ONLINE_SIN_BUSINESS
    private CanalVenta canalVenta;
    
    // Notas adicionales
    private String notas;
    
    // Sistema de aprobación de ventas
    // Estado de aprobación: PENDIENTE_APROBACION, APROBADA, RECHAZADA
    @Builder.Default
    private String estadoAprobacion = "APROBADA"; // Por defecto APROBADA para admins
    
    // Usuario que aprobó la venta (admin o admin_local)
    private String aprobadoPor;
    
    // Fecha de aprobación
    private LocalDateTime fechaAprobacion;
    
    // Motivo de rechazo (opcional, solo si estadoAprobacion = RECHAZADA)
    private String motivoRechazo;

    // Gestión de encargos
    @Builder.Default
    private String tipoVenta = "NORMAL"; // "NORMAL" o "ENCARGO"

    @Builder.Default
    private Double totalAbonado = 0.0; // Suma de todos los abonos realizados

    // Saldo pendiente: totalCLP - totalAbonado
    private Double saldoPendiente;

    // Estado de pago: PENDIENTE, ABONADO_PARCIAL, PAGADO_COMPLETO, CANCELADO
    private String estadoPago;
}
