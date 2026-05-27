package com.recred.backend.infrastructure.push;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.repositories.UserDeviceTokenRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class FirebasePushNotificationSenderTest {

    @Test
    void doNothingWhenFirebaseIsNotConfigured() {
        UserDeviceTokenRepository tokenRepository = mock(UserDeviceTokenRepository.class);
        FirebaseMessagingGateway gateway = mock(FirebaseMessagingGateway.class);
        FirebasePushNotificationSender sender = new FirebasePushNotificationSender(
                Optional.empty(),
                tokenRepository,
                gateway
        );
        PushNotification notification = notification();

        sender.send(notification);

        verify(tokenRepository, never()).findActiveTokensByUserId(notification.userId());
    }

    @Test
    void queryActiveTokensWhenFirebaseIsConfigured() {
        UserDeviceTokenRepository tokenRepository = mock(UserDeviceTokenRepository.class);
        FirebaseMessagingGateway gateway = mock(FirebaseMessagingGateway.class);
        FirebaseApp firebaseApp = mock(FirebaseApp.class);
        FirebasePushNotificationSender sender = new FirebasePushNotificationSender(
                Optional.of(firebaseApp),
                tokenRepository,
                gateway
        );
        PushNotification notification = notification();
        when(tokenRepository.findActiveTokensByUserId(notification.userId()))
                .thenReturn(List.of());

        sender.send(notification);

        verify(tokenRepository).findActiveTokensByUserId(notification.userId());
    }

    @Test
    void sendPushToEachActiveToken() throws FirebaseMessagingException {
        UserDeviceTokenRepository tokenRepository = mock(UserDeviceTokenRepository.class);
        FirebaseMessagingGateway gateway = mock(FirebaseMessagingGateway.class);
        FirebaseApp firebaseApp = mock(FirebaseApp.class);
        FirebasePushNotificationSender sender = new FirebasePushNotificationSender(
                Optional.of(firebaseApp),
                tokenRepository,
                gateway
        );
        PushNotification notification = notificationWithNullData();
        when(tokenRepository.findActiveTokensByUserId(notification.userId()))
                .thenReturn(List.of("token-1", "token-2"));

        sender.send(notification);

        verify(gateway, times(2)).send(
                org.mockito.ArgumentMatchers.eq(firebaseApp),
                org.mockito.ArgumentMatchers.any(Message.class)
        );
    }

    @Test
    void continueWhenFirebaseRejectsToken() throws FirebaseMessagingException {
        UserDeviceTokenRepository tokenRepository = mock(UserDeviceTokenRepository.class);
        FirebaseMessagingGateway gateway = mock(FirebaseMessagingGateway.class);
        FirebaseApp firebaseApp = mock(FirebaseApp.class);
        FirebasePushNotificationSender sender = new FirebasePushNotificationSender(
                Optional.of(firebaseApp),
                tokenRepository,
                gateway
        );
        PushNotification notification = notification();
        when(tokenRepository.findActiveTokensByUserId(notification.userId()))
                .thenReturn(List.of("token-1"));
        doThrow(mock(FirebaseMessagingException.class)).when(gateway)
                .send(
                        org.mockito.ArgumentMatchers.eq(firebaseApp),
                        org.mockito.ArgumentMatchers.any(Message.class)
                );

        sender.send(notification);

        verify(gateway).send(
                org.mockito.ArgumentMatchers.eq(firebaseApp),
                org.mockito.ArgumentMatchers.any(Message.class)
        );
    }

    private PushNotification notification() {
        return new PushNotification(
                UUID.randomUUID(),
                "Titulo",
                "Mensaje",
                Map.of("tipo", "SUGERENCIA_IA")
        );
    }

    private PushNotification notificationWithNullData() {
        return new PushNotification(
                UUID.randomUUID(),
                "Titulo",
                "Mensaje",
                null
        );
    }
}
