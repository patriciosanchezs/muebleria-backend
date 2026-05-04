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
@Document(collection = "push_subscriptions")
public class PushSubscription {

    @Id
    private String id;

    @Indexed
    private String username;

    private String endpoint;

    private String p256dhKey;

    private String authKey;

    private String userAgent;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
