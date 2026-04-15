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
    
    // Método de pago: EFECTIVO, TRANSFERENCIA, DEBITO, CREDITO
    private String metodoPago;
    
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
    
    // Notas adicionales
    private String notas;
}
