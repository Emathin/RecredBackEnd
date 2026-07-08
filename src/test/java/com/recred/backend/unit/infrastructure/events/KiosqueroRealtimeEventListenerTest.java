package com.recred.backend.unit.infrastructure.events;

import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimeEventType;
import com.recred.backend.infrastructure.events.KiosqueroRealtimeEventListener;
import com.recred.backend.infrastructure.web.KiosqueroRealtimeEventStream;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KiosqueroRealtimeEventListenerTest {

    private final KiosqueroRealtimeEventStream eventStream =
            mock(KiosqueroRealtimeEventStream.class);
    private final KiosqueroRealtimeEventListener listener =
            new KiosqueroRealtimeEventListener(eventStream);

    @Test
    void handle_ShouldPublishEventToStream() {
        KiosqueroRealtimeEvent event = KiosqueroRealtimeEvent.productAlert(
                UUID.randomUUID(),
                KiosqueroRealtimeEventType.ORDER_UPDATED,
                null,
                "Pedido actualizado"
        );

        listener.handle(event);

        verify(eventStream).publish(event);
    }
}

