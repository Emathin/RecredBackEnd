package com.recred.backend.infrastructure.push;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.repositories.UserDeviceTokenRepository;
import com.recred.backend.domain.services.PushNotificationSender;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FirebasePushNotificationSender implements PushNotificationSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            FirebasePushNotificationSender.class);

    private final Optional<FirebaseApp> firebaseApp;
    private final UserDeviceTokenRepository userDeviceTokenRepository;
    private final FirebaseMessagingGateway firebaseMessagingGateway;

    public FirebasePushNotificationSender(
            Optional<FirebaseApp> firebaseApp,
            UserDeviceTokenRepository userDeviceTokenRepository,
            FirebaseMessagingGateway firebaseMessagingGateway
    ) {
        this.firebaseApp = firebaseApp;
        this.userDeviceTokenRepository = userDeviceTokenRepository;
        this.firebaseMessagingGateway = firebaseMessagingGateway;
    }

    @Override
    public void send(PushNotification notification) {
        if (firebaseApp.isEmpty()) {
            return;
        }
        userDeviceTokenRepository.findActiveTokensByUserId(notification.userId())
                .forEach(token -> sendToToken(notification, token));
    }

    private void sendToToken(PushNotification notification, String token) {
        try {
            firebaseMessagingGateway.send(firebaseApp.orElseThrow(), message(
                    notification,
                    token
            ));
        } catch (FirebaseMessagingException exception) {
            LOGGER.warn("Firebase push notification failed: {}", exception.getMessage());
        }
    }

    private Message message(PushNotification notification, String token) {
        return Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(notification.title())
                        .setBody(notification.message())
                        .build())
                .putAllData(data(notification))
                .build();
    }

    private Map<String, String> data(PushNotification notification) {
        return notification.data() == null ? Map.of() : notification.data();
    }
}
