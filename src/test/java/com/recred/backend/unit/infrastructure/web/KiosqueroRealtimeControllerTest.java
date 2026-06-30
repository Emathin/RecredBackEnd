package com.recred.backend.unit.infrastructure.web;

import java.util.UUID;

import com.recred.backend.infrastructure.web.KiosqueroAccessGuard;
import com.recred.backend.infrastructure.web.KiosqueroRealtimeController;
import com.recred.backend.infrastructure.web.KiosqueroRealtimeEventStream;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KiosqueroRealtimeControllerTest {

    private final KiosqueroRealtimeEventStream eventStream =
            mock(KiosqueroRealtimeEventStream.class);
    private final KiosqueroAccessGuard kiosqueroAccessGuard = mock(KiosqueroAccessGuard.class);
    private final KiosqueroRealtimeController controller =
            new KiosqueroRealtimeController(eventStream, kiosqueroAccessGuard);

    @Test
    void events_ShouldSubscribeToBuffetStream() {
        UUID buffetId = UUID.randomUUID();
        SseEmitter emitter = new SseEmitter();

        when(eventStream.subscribe(buffetId)).thenReturn(emitter);

        SseEmitter result = controller.events(buffetId, null);

        assertSame(emitter, result);
        verify(eventStream).subscribe(buffetId);
    }
}

