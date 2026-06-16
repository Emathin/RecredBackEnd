package com.recred.backend.integration.adapters.push;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.recred.backend.infrastructure.push.FirebaseMessagingGatewayImpl;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;

class FirebaseMessagingGatewayImplTest {

    @Test
    void sendMessageUsingFirebaseMessagingInstance() throws FirebaseMessagingException {
        FirebaseMessagingGatewayImpl gateway = new FirebaseMessagingGatewayImpl();
        FirebaseApp firebaseApp = Mockito.mock(FirebaseApp.class);
        FirebaseMessaging firebaseMessaging = Mockito.mock(FirebaseMessaging.class);
        Message message = Message.builder()
                .setToken("device-token")
                .build();

        try (MockedStatic<FirebaseMessaging> messaging = Mockito.mockStatic(
                FirebaseMessaging.class)) {
            messaging.when(() -> FirebaseMessaging.getInstance(firebaseApp))
                    .thenReturn(firebaseMessaging);

            gateway.send(firebaseApp, message);

            verify(firebaseMessaging).send(message);
        }
    }
}
