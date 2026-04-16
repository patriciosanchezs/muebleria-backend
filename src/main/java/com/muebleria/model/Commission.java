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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "commissions")
public class Commission {
    @Id
    private String id;
    
    private String saleId;
    private String vendedorUsername;
    private LocalDateTime fechaVenta;
    private Local local;
    private List<CommissionItem> items;
    private double totalComision;
}
