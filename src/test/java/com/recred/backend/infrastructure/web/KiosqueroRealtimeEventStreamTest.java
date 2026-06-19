package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.events.KiosqueroRealtimeChangeKind;
import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KiosqueroRealtimeEventStreamTest {

    @Test
    void subscribe_WhenBuffetIdIsMissing_ShouldFail() {
        KiosqueroRealtimeEventStream stream = new KiosqueroRealtimeEventStream();

        assertThrows(IllegalArgumentException.class, () -> stream.subscribe(null));
    }

    @Test
    void publish_WhenEventIsNullOrWithoutBuffet_ShouldIgnoreIt() {
        KiosqueroRealtimeEventStream stream = new KiosqueroRealtimeEventStream();

        stream.publish(null);
        stream.publish(event(null));

        assertTrue(emitters(stream).isEmpty());
    }

    @Test
    void publish_WhenEventHasNoRegisteredEmitter_ShouldIgnoreIt() {
        KiosqueroRealtimeEventStream stream = new KiosqueroRealtimeEventStream();

        stream.publish(event(UUID.randomUUID()));

        assertTrue(emitters(stream).isEmpty());
    }

    @Test
    void subscribe_ShouldRegisterEmitterAndAllowRemoval() {
        UUID buffetId = UUID.randomUUID();
        KiosqueroRealtimeEventStream stream = new KiosqueroRealtimeEventStream();

        SseEmitter emitter = stream.subscribe(buffetId);

        assertEquals(1, emitters(stream).get(buffetId).size());

        ReflectionTestUtils.invokeMethod(stream, "remove", buffetId, emitter);

        assertTrue(emitters(stream).isEmpty());
    }

    @Test
    void remove_WhenBuffetHasNoEmitters_ShouldDoNothing() {
        KiosqueroRealtimeEventStream stream = new KiosqueroRealtimeEventStream();

        ReflectionTestUtils.invokeMethod(
                stream,
                "remove",
                UUID.randomUUID(),
                new SseEmitter()
        );

        assertTrue(emitters(stream).isEmpty());
    }

    @Test
    void publish_WhenEmitterExists_ShouldSendEvent() {
        UUID buffetId = UUID.randomUUID();
        KiosqueroRealtimeEventStream stream = new KiosqueroRealtimeEventStream();
        TestEmitter emitter = new TestEmitter(false);
        emitters(stream).put(buffetId, new CopyOnWriteArrayList<>(List.of(emitter)));

        stream.publish(event(buffetId));

        assertEquals(1, emitter.sentEvents);
        assertTrue(emitters(stream).containsKey(buffetId));
    }

    @Test
    void publish_WhenEmitterFails_ShouldRemoveIt() {
        UUID buffetId = UUID.randomUUID();
        KiosqueroRealtimeEventStream stream = new KiosqueroRealtimeEventStream();
        TestEmitter emitter = new TestEmitter(true);
        emitters(stream).put(buffetId, new CopyOnWriteArrayList<>(List.of(emitter)));

        stream.publish(event(buffetId));

        assertEquals(1, emitter.sentEvents);
        assertTrue(emitter.completedWithError);
        assertTrue(emitters(stream).isEmpty());
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, CopyOnWriteArrayList<SseEmitter>> emitters(
            KiosqueroRealtimeEventStream stream
    ) {
        return (Map<UUID, CopyOnWriteArrayList<SseEmitter>>) ReflectionTestUtils.getField(
                stream,
                "emitters"
        );
    }

    private KiosqueroRealtimeEvent event(UUID buffetId) {
        return KiosqueroRealtimeEvent.dashboardChanged(
                buffetId,
                LocalDate.of(2026, 6, 15),
                KiosqueroRealtimeChangeKind.ORDER_CHANGED,
                "Cambio"
        );
    }

    private static class TestEmitter extends SseEmitter {
        private final boolean fail;
        private int sentEvents;
        private boolean completedWithError;

        TestEmitter(boolean fail) {
            this.fail = fail;
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            sentEvents++;
            if (fail)
                throw new IOException("send failed");
        }

        @Override
        public void completeWithError(Throwable ex) {
            completedWithError = true;
        }
    }
}
