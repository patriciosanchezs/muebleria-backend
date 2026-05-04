package com.muebleria.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    @Indexed
    private String username;

    private String title;

    private String body;

    private String url;

    private String saleId;

    @Builder.Default
    private boolean read = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
