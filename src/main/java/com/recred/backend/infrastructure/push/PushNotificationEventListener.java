package com.recred.backend.infrastructure.push;

import com.recred.backend.domain.services.PushNotificationSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PushNotificationEventListener {

    private final PushNotificationSender pushNotificationSender;

    public PushNotificationEventListener(PushNotificationSender pushNotificationSender) {
        this.pushNotificationSender = pushNotificationSender;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void send(PushNotificationRequestedEvent event) {
        pushNotificationSender.send(event.notification());
    }
}
