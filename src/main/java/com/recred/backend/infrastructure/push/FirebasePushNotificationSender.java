package com.recred.backend.infrastructure.push;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.repositories.UserDeviceTokenRepository;
import com.recred.backend.domain.services.PushNotificationSender;
import java.util.ArrayList;
import java.util.List;
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

    public FirebasePushNotificationSender(
            @org.springframework.beans.factory.annotation.Autowired(required = false) FirebaseApp firebaseApp,
            UserDeviceTokenRepository userDeviceTokenRepository
    ) {
        this.firebaseApp = Optional.ofNullable(firebaseApp);
        this.userDeviceTokenRepository = userDeviceTokenRepository;
    }

    @Override
    public void send(PushNotification notification) {
        if (firebaseApp.isEmpty()) {
            LOGGER.warn("FirebaseApp no está inicializado. "
                    + "Se omite el envío de la notificación.");
            return;
        }
        List<String> tokens = userDeviceTokenRepository.findActiveTokensByUserId(
                notification.userId());

        if (tokens == null || tokens.isEmpty()) {
            LOGGER.debug("No hay tokens activos para el usuario id: {}", notification.userId());
            return;
        }

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens) 
                .setNotification(Notification.builder()
                        .setTitle(notification.title())
                        .setBody(notification.message())
                        .build())
                .putAllData(data(notification))
                .build();

        try {
            FirebaseMessaging messaging = FirebaseMessaging.getInstance(firebaseApp.get());
            BatchResponse response = messaging.sendEachForMulticast(message);

            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                List<String> invalidTokens = new ArrayList<>();

                for (int i = 0; i < responses.size(); i++) {
                    SendResponse sendResponse = responses.get(i);
                    
                    if (!sendResponse.isSuccessful()) {
                        String failedToken = tokens.get(i); 
                        MessagingErrorCode errorCode = sendResponse.getException()
                                .getMessagingErrorCode();

                        if (isTokenInvalid(errorCode)) {
                            invalidTokens.add(failedToken);
                            LOGGER.info("Token inactivo o inválido detectado, marcando "
                                    + "para limpieza: {}", failedToken);
                        } else {
                            LOGGER.warn("Fallo al enviar notificación al token {}: {}",
                                    failedToken, sendResponse.getException().getMessage());
                        }
                    }
                }

                if (!invalidTokens.isEmpty()) {
                    userDeviceTokenRepository.deactivateTokens(invalidTokens); 
                    LOGGER.info("Limpieza ejecutada: {} tokens han sido desactivados.",
                            invalidTokens.size());
                }
            } else {
                LOGGER.debug("Notificación enviada exitosamente a todos los dispositivos "
                        + "({} en total)", tokens.size());
            }

        } catch (FirebaseMessagingException exception) {
            LOGGER.error("Fallo general y crítico al contactar con Firebase: {}",
                    exception.getMessage(), exception);
        }
    }

    private boolean isTokenInvalid(MessagingErrorCode errorCode) {
        return errorCode == MessagingErrorCode.UNREGISTERED || 
               errorCode == MessagingErrorCode.INVALID_ARGUMENT;
    }

    private Map<String, String> data(PushNotification notification) {
        return notification.data() == null ? Map.of() : notification.data();
    }
}
