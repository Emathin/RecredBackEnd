package com.recred.backend.infrastructure.push;

import com.recred.backend.domain.models.PushNotification;
import com.recred.backend.domain.services.PushNotificationSender;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PushNotificationEventListenerTest {

    @Test
    void sendPushNotificationFromEvent() {
        PushNotificationSender sender = mock(PushNotificationSender.class);
        PushNotificationEventListener listener = new PushNotificationEventListener(sender);
        PushNotification notification = new PushNotification(
                UUID.randomUUID(),
                "Titulo",
                "Mensaje",
                Map.of("tipo", "SUGERENCIA_IA")
        );

        listener.send(new PushNotificationRequestedEvent(notification));

        verify(sender).send(notification);
    }
}
