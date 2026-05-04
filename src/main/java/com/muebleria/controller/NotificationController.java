package com.muebleria.controller;

import com.muebleria.dto.PushSubscriptionRequest;
import com.muebleria.dto.UnsubscribeRequest;
import com.muebleria.model.Notification;
import com.muebleria.service.PushNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final PushNotificationService pushNotificationService;

    @GetMapping("/vapid-public-key")
    public ResponseEntity<Map<String, String>> getVapidPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", pushNotificationService.getVapidPublicKey()));
    }

    @PostMapping("/subscribe")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> subscribe(
            @RequestBody PushSubscriptionRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String username = authentication.getName();
        String userAgent = httpRequest.getHeader("User-Agent");
        pushNotificationService.subscribe(username, request.getEndpoint(), request.getP256dh(), request.getAuth(), userAgent);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/subscribe")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unsubscribe(@RequestBody UnsubscribeRequest request) {
        pushNotificationService.unsubscribe(request.getEndpoint());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Notification>> getNotifications(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(pushNotificationService.getNotifications(username));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        String username = authentication.getName();
        long count = pushNotificationService.getUnreadCount(username);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@PathVariable String id, Authentication authentication) {
        String username = authentication.getName();
        pushNotificationService.markAsRead(id, username);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        String username = authentication.getName();
        pushNotificationService.markAllAsRead(username);
        return ResponseEntity.ok().build();
    }
}
