package com.recred.backend.infrastructure.push;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.repositories.UserDeviceTokenRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

class FirebasePushNotificationSenderTest {

    @Test
    void doNothingWhenFirebaseIsNotConfigured() {
        UserDeviceTokenRepository tokenRepository = mock(UserDeviceTokenRepository.class);
        FirebasePushNotificationSender sender = new FirebasePushNotificationSender(
                Optional.empty(),
                tokenRepository
        );
        PushNotification notification = notification();

        sender.send(notification);

        verify(tokenRepository, never()).findActiveTokensByUserId(notification.userId());
    }

    @Test
    void queryActiveTokensWhenFirebaseIsConfigured() {
        UserDeviceTokenRepository tokenRepository = mock(UserDeviceTokenRepository.class);
        FirebaseApp firebaseApp = mock(FirebaseApp.class);
        FirebasePushNotificationSender sender = new FirebasePushNotificationSender(
                Optional.of(firebaseApp),
                tokenRepository
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
        FirebaseApp firebaseApp = mock(FirebaseApp.class);
        FirebasePushNotificationSender sender = new FirebasePushNotificationSender(
                Optional.of(firebaseApp),
                tokenRepository
        );
        PushNotification notification = notificationWithNullData();
        when(tokenRepository.findActiveTokensByUserId(notification.userId()))
                .thenReturn(List.of("token-1", "token-2"));

        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getFailureCount()).thenReturn(0);
        when(messaging.sendEachForMulticast(org.mockito.ArgumentMatchers.any(MulticastMessage.class)))
                .thenReturn(batchResponse);

        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            mocked.when(() -> FirebaseMessaging.getInstance(firebaseApp)).thenReturn(messaging);
            
            sender.send(notification);
            
            verify(messaging).sendEachForMulticast(org.mockito.ArgumentMatchers.any(MulticastMessage.class));
        }
    }

    @Test
    void sendDataOnlyPushWhenNotificationHasNoTitleOrMessage()
            throws FirebaseMessagingException {
        UserDeviceTokenRepository tokenRepository = mock(UserDeviceTokenRepository.class);
        FirebaseApp firebaseApp = mock(FirebaseApp.class);
        FirebasePushNotificationSender sender = new FirebasePushNotificationSender(
                Optional.of(firebaseApp),
                tokenRepository
        );
        PushNotification notification = new PushNotification(
                UUID.randomUUID(),
                null,
                null,
                Map.of("tipo", "DATA_ONLY")
        );
        when(tokenRepository.findActiveTokensByUserId(notification.userId()))
                .thenReturn(List.of("token-1"));
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getFailureCount()).thenReturn(0);
        when(messaging.sendEachForMulticast(
                org.mockito.ArgumentMatchers.any(MulticastMessage.class)
        )).thenReturn(batchResponse);

        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            mocked.when(() -> FirebaseMessaging.getInstance(firebaseApp)).thenReturn(messaging);

            sender.send(notification);

            verify(messaging).sendEachForMulticast(
                    org.mockito.ArgumentMatchers.any(MulticastMessage.class)
            );
        }
    }

    @Test
    void continueWhenFirebaseRejectsToken() throws FirebaseMessagingException {
        UserDeviceTokenRepository tokenRepository = mock(UserDeviceTokenRepository.class);
        FirebaseApp firebaseApp = mock(FirebaseApp.class);
        FirebasePushNotificationSender sender = new FirebasePushNotificationSender(
                Optional.of(firebaseApp),
                tokenRepository
        );
        PushNotification notification = notification();
        when(tokenRepository.findActiveTokensByUserId(notification.userId()))
                .thenReturn(List.of("token-1"));

        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        when(messaging.sendEachForMulticast(org.mockito.ArgumentMatchers.any(MulticastMessage.class)))
                .thenThrow(mock(FirebaseMessagingException.class));

        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            mocked.when(() -> FirebaseMessaging.getInstance(firebaseApp)).thenReturn(messaging);
            
            sender.send(notification);
            
            verify(messaging).sendEachForMulticast(org.mockito.ArgumentMatchers.any(MulticastMessage.class));
        }
    }

    @Test
    void deactivateTokensWhenFirebaseRejectsWithUnregistered() throws FirebaseMessagingException {
        UserDeviceTokenRepository tokenRepository = mock(UserDeviceTokenRepository.class);
        FirebaseApp firebaseApp = mock(FirebaseApp.class);
        FirebasePushNotificationSender sender = new FirebasePushNotificationSender(
                Optional.of(firebaseApp),
                tokenRepository
        );
        PushNotification notification = notification();
        when(tokenRepository.findActiveTokensByUserId(notification.userId()))
                .thenReturn(List.of("token-1"));

        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getFailureCount()).thenReturn(1);
        
        SendResponse sendResponse = mock(SendResponse.class);
        when(sendResponse.isSuccessful()).thenReturn(false);
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(sendResponse.getException()).thenReturn(exception);
        
        when(batchResponse.getResponses()).thenReturn(List.of(sendResponse));
        when(messaging.sendEachForMulticast(org.mockito.ArgumentMatchers.any(MulticastMessage.class)))
                .thenReturn(batchResponse);

        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            mocked.when(() -> FirebaseMessaging.getInstance(firebaseApp)).thenReturn(messaging);
            
            sender.send(notification);
            
            verify(messaging).sendEachForMulticast(org.mockito.ArgumentMatchers.any(MulticastMessage.class));
            verify(tokenRepository).deactivateTokens(List.of("token-1"));
        }
    }

    @Test
    void keepTokenActiveWhenFirebaseRejectsWithRetryableError()
            throws FirebaseMessagingException {
        UserDeviceTokenRepository tokenRepository = mock(UserDeviceTokenRepository.class);
        FirebaseApp firebaseApp = mock(FirebaseApp.class);
        FirebasePushNotificationSender sender = new FirebasePushNotificationSender(
                Optional.of(firebaseApp),
                tokenRepository
        );
        PushNotification notification = notification();
        when(tokenRepository.findActiveTokensByUserId(notification.userId()))
                .thenReturn(List.of("token-1"));
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getFailureCount()).thenReturn(1);
        SendResponse sendResponse = mock(SendResponse.class);
        when(sendResponse.isSuccessful()).thenReturn(false);
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INTERNAL);
        when(exception.getMessage()).thenReturn("firebase internal error");
        when(sendResponse.getException()).thenReturn(exception);
        when(batchResponse.getResponses()).thenReturn(List.of(sendResponse));
        when(messaging.sendEachForMulticast(
                org.mockito.ArgumentMatchers.any(MulticastMessage.class)
        )).thenReturn(batchResponse);

        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            mocked.when(() -> FirebaseMessaging.getInstance(firebaseApp)).thenReturn(messaging);

            sender.send(notification);

            verify(tokenRepository, never()).deactivateTokens(List.of("token-1"));
        }
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
