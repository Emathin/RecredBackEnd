package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.infrastructure.web.*;

import com.recred.backend.domain.events.KiosqueroRealtimeChangeKind;
import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KiosqueroRealtimeEventStreamTest {

    @Test
    void subscribeWhenBuffetIdIsMissingShouldFail() {
        KiosqueroRealtimeEventStream stream = new KiosqueroRealtimeEventStream();

        whenSubscribingWithNull(stream);
    }

    @Test
    void publishWhenEventIsNullOrWithoutBuffetShouldIgnoreIt() {
        KiosqueroRealtimeEventStream stream = new KiosqueroRealtimeEventStream();

        whenPublishingNullAndEmptyEvents(stream);

        thenNoEmittersExist(stream);
    }

    @Test
    void publishWhenEventHasNoRegisteredEmitterShouldIgnoreIt() {
        KiosqueroRealtimeEventStream stream = new KiosqueroRealtimeEventStream();

        whenPublishingEventForRandomBuffet(stream);

        thenNoEmittersExist(stream);
    }

    @Test
    void subscribeShouldRegisterEmitterAndAllowRemoval() {
        UUID buffetId = UUID.randomUUID();
        KiosqueroRealtimeEventStream stream = new KiosqueroRealtimeEventStream();

        SseEmitter emitter = whenSubscribing(stream, buffetId);
        thenEmitterIsRegistered(stream, buffetId);

        whenRemovingEmitter(stream, buffetId, emitter);
        thenNoEmittersExist(stream);
    }

    @Test
    void removeWhenBuffetHasNoEmittersShouldDoNothing() {
        KiosqueroRealtimeEventStream stream = new KiosqueroRealtimeEventStream();

        whenRemovingEmitter(stream, UUID.randomUUID(), new SseEmitter());

        thenNoEmittersExist(stream);
    }

    @Test
    void publishWhenEmitterExistsShouldSendEvent() {
        UUID buffetId = UUID.randomUUID();
        KiosqueroRealtimeEventStream stream = new KiosqueroRealtimeEventStream();
        TestEmitter emitter = givenEmitter(stream, buffetId, false);

        whenPublishingEvent(stream, buffetId);

        thenEventIsSent(emitter);
        thenEmitterRemainsRegistered(stream, buffetId);
    }

    @Test
    void publishWhenEmitterFailsShouldRemoveIt() {
        UUID buffetId = UUID.randomUUID();
        KiosqueroRealtimeEventStream stream = new KiosqueroRealtimeEventStream();
        TestEmitter emitter = givenEmitter(stream, buffetId, true);

        whenPublishingEvent(stream, buffetId);

        thenEventIsSent(emitter);
        thenEmitterFailsAndIsRemoved(emitter, stream);
    }

    private void whenSubscribingWithNull(KiosqueroRealtimeEventStream stream) {
        assertThrows(IllegalArgumentException.class, () -> stream.subscribe(null));
    }

    private void whenPublishingNullAndEmptyEvents(KiosqueroRealtimeEventStream stream) {
        stream.publish(null);
        stream.publish(event(null));
    }

    private void whenPublishingEventForRandomBuffet(KiosqueroRealtimeEventStream stream) {
        stream.publish(event(UUID.randomUUID()));
    }

    private SseEmitter whenSubscribing(KiosqueroRealtimeEventStream stream, UUID buffetId) {
        return stream.subscribe(buffetId);
    }

    private void whenRemovingEmitter(KiosqueroRealtimeEventStream stream, UUID buffetId, SseEmitter emitter) {
        ReflectionTestUtils.invokeMethod(stream, "remove", buffetId, emitter);
    }

    private void whenPublishingEvent(KiosqueroRealtimeEventStream stream, UUID buffetId) {
        stream.publish(event(buffetId));
    }

    private TestEmitter givenEmitter(KiosqueroRealtimeEventStream stream, UUID buffetId, boolean fail) {
        TestEmitter emitter = new TestEmitter(fail);
        emitters(stream).put(buffetId, new CopyOnWriteArrayList<>(List.of(emitter)));
        return emitter;
    }

    private void thenNoEmittersExist(KiosqueroRealtimeEventStream stream) {
        assertTrue(emitters(stream).isEmpty());
    }

    private void thenEmitterIsRegistered(KiosqueroRealtimeEventStream stream, UUID buffetId) {
        assertEquals(1, emitters(stream).get(buffetId).size());
    }

    private void thenEmitterRemainsRegistered(KiosqueroRealtimeEventStream stream, UUID buffetId) {
        assertTrue(emitters(stream).containsKey(buffetId));
    }

    private void thenEventIsSent(TestEmitter emitter) {
        assertEquals(1, emitter.sentEvents);
    }

    private void thenEmitterFailsAndIsRemoved(TestEmitter emitter, KiosqueroRealtimeEventStream stream) {
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
