package com.recred.backend.unit.infrastructure.web;

import java.util.UUID;

import com.recred.backend.infrastructure.web.KiosqueroAccessGuard;
import com.recred.backend.infrastructure.web.KiosqueroRealtimeController;
import com.recred.backend.infrastructure.web.KiosqueroRealtimeEventStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KiosqueroRealtimeControllerTest {

    private KiosqueroRealtimeEventStream eventStream;
    private KiosqueroAccessGuard kiosqueroAccessGuard;
    private KiosqueroRealtimeController controller;
    
    private final UUID BUFFET_ID = UUID.randomUUID();
    private SseEmitter expectedEmitter;

    @BeforeEach
    void setUp() {
        eventStream = mock(KiosqueroRealtimeEventStream.class);
        kiosqueroAccessGuard = mock(KiosqueroAccessGuard.class);
        controller = new KiosqueroRealtimeController(eventStream, kiosqueroAccessGuard);
    }

    @Test
    void eventsShouldSubscribeToBuffetStream() {
        givenEmitterSubscription();
        
        SseEmitter result = whenGettingEvents();
        
        thenEmitterIsReturned(result);
    }

    private void givenEmitterSubscription() {
        expectedEmitter = new SseEmitter();
        when(eventStream.subscribe(BUFFET_ID)).thenReturn(expectedEmitter);
    }

    private SseEmitter whenGettingEvents() {
        return controller.events(BUFFET_ID, null);
    }

    private void thenEmitterIsReturned(SseEmitter result) {
        assertSame(expectedEmitter, result);
        verify(eventStream).subscribe(BUFFET_ID);
    }
}
