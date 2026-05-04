package com.muebleria.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PushSubscriptionRequest {
    private String endpoint;
    private String p256dh;
    private String auth;
}
