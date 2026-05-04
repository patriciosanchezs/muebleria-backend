package com.muebleria.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muebleria.model.Notification;
import com.muebleria.model.PushSubscription;
import com.muebleria.model.Role;
import com.muebleria.model.User;
import com.muebleria.repository.NotificationRepository;
import com.muebleria.repository.PushSubscriptionRepository;
import com.muebleria.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${vapid.public-key}")
    private String vapidPublicKey;

    @Value("${vapid.private-key}")
    private String vapidPrivateKey;

    @Value("${vapid.subject}")
    private String vapidSubject;

    private PushService pushService;

    @PostConstruct
    public void init() {
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
            log.info("PushNotificationService inicializado correctamente");
        } catch (GeneralSecurityException e) {
            log.error("Error inicializando PushNotificationService: {}", e.getMessage());
        }
    }

    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    public void subscribe(String username, String endpoint, String p256dh, String auth, String userAgent) {
        if (pushSubscriptionRepository.existsByUsernameAndEndpoint(username, endpoint)) {
            log.debug("Suscripcion ya existe para usuario {} endpoint {}", username, endpoint);
            return;
        }

        PushSubscription subscription = PushSubscription.builder()
                .username(username)
                .endpoint(endpoint)
                .p256dhKey(p256dh)
                .authKey(auth)
                .userAgent(userAgent)
                .build();

        pushSubscriptionRepository.save(subscription);
        log.info("Suscripcion push guardada para usuario {}", username);
    }

    public void unsubscribe(String endpoint) {
        pushSubscriptionRepository.deleteByEndpoint(endpoint);
        log.info("Suscripcion push eliminada para endpoint");
    }

    public void sendPushToUser(String username, String title, String body, String url, String saleId) {
        Notification notification = Notification.builder()
                .username(username)
                .title(title)
                .body(body)
                .url(url)
                .saleId(saleId)
                .build();
        notificationRepository.save(notification);

        List<PushSubscription> subscriptions = pushSubscriptionRepository.findByUsername(username);
        for (PushSubscription sub : subscriptions) {
            sendPush(sub, title, body, url);
        }
    }

    public void sendPushToUsers(List<String> usernames, String title, String body, String url, String saleId) {
        for (String username : usernames) {
            sendPushToUser(username, title, body, url, saleId);
        }
    }

    private void sendPush(PushSubscription subscription, String title, String body, String url) {
        try {
            if (pushService == null) {
                log.warn("PushService no inicializado, no se puede enviar push");
                return;
            }

            Map<String, Object> payload = Map.of(
                    "title", title,
                    "body", body,
                    "url", url != null ? url : "/",
                    "icon", "/pwa-192x192.png"
            );

            String jsonPayload = objectMapper.writeValueAsString(payload);

            Subscription.Keys keys = new Subscription.Keys(
                    subscription.getP256dhKey(),
                    subscription.getAuthKey()
            );
            Subscription webPushSub = new Subscription(subscription.getEndpoint(), keys);

            nl.martijndwars.webpush.Notification pushNotification =
                    new nl.martijndwars.webpush.Notification(webPushSub, jsonPayload);

            pushService.send(pushNotification);
            log.debug("Push enviado a {}", subscription.getUsername());
        } catch (Exception e) {
            log.error("Error enviando push a {}: {}", subscription.getUsername(), e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("410")) {
                pushSubscriptionRepository.deleteByEndpoint(subscription.getEndpoint());
                log.info("Suscripcion obsoleta eliminada para {}", subscription.getUsername());
            }
        }
    }

    public List<Notification> getNotifications(String username) {
        return notificationRepository.findTop20ByUsernameOrderByCreatedAtDesc(username);
    }

    public long getUnreadCount(String username) {
        return notificationRepository.countByUsernameAndReadFalse(username);
    }

    public void markAsRead(String notificationId, String username) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getUsername().equals(username)) {
                notification.setRead(true);
                notificationRepository.save(notification);
            }
        });
    }

    public void markAllAsRead(String username) {
        List<Notification> notifications = notificationRepository.findTop20ByUsernameOrderByCreatedAtDesc(username);
        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                notification.setRead(true);
            }
        }
        notificationRepository.saveAll(notifications);
    }

    public List<String> findAdminUsernamesByLocal(String localId) {
        return userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(u -> u.getRole() == Role.ADMINISTRADOR ||
                        u.getRole() == Role.ADMIN_LOCAL ||
                        u.getRole() == Role.ENCARGADO_LOCAL)
                .filter(u -> {
                    if (u.getRole() == Role.ADMINISTRADOR) return true;
                    return u.getLocalIds() != null && u.getLocalIds().contains(localId);
                })
                .map(User::getUsername)
                .toList();
    }
}
