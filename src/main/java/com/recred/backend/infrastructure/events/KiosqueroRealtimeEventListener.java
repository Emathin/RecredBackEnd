package com.recred.backend.infrastructure.events;

import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.infrastructure.web.KiosqueroRealtimeEventStream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class KiosqueroRealtimeEventListener {

    private final KiosqueroRealtimeEventStream eventStream;

    public KiosqueroRealtimeEventListener(KiosqueroRealtimeEventStream eventStream) {
        this.eventStream = eventStream;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(KiosqueroRealtimeEvent event) {
        eventStream.publish(event);
    }
}
