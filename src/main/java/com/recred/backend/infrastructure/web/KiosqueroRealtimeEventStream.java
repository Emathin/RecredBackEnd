package com.recred.backend.infrastructure.web;

import com.recred.backend.application.KiosqueroRealtimeEventResponse;
import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class KiosqueroRealtimeEventStream {

    private static final long TIMEOUT = Duration.ofMinutes(30).toMillis();

    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emitters =
            new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID buffetId) {
        if (buffetId == null) {
            throw new IllegalArgumentException("buffetId is required");
        }

        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.computeIfAbsent(buffetId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(buffetId, emitter));
        emitter.onTimeout(() -> remove(buffetId, emitter));
        emitter.onError(error -> remove(buffetId, emitter));
        return emitter;
    }

    public void publish(KiosqueroRealtimeEvent event) {
        if (event == null || event.buffetId() == null) {
            return;
        }

        List<SseEmitter> buffetEmitters = emitters.get(event.buffetId());
        if (buffetEmitters == null) {
            return;
        }

        buffetEmitters.forEach(emitter -> send(event.buffetId(), emitter, event));
    }

    private void send(UUID buffetId, SseEmitter emitter, KiosqueroRealtimeEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .id(UUID.randomUUID().toString())
                    .name(eventName(event))
                    .data(KiosqueroRealtimeEventResponse.fromDomain(event)));
        } catch (IOException exception) {
            emitter.completeWithError(exception);
            remove(buffetId, emitter);
        }
    }

    private String eventName(KiosqueroRealtimeEvent event) {
        return event.type().name().toLowerCase(Locale.ROOT);
    }

    private void remove(UUID buffetId, SseEmitter emitter) {
        List<SseEmitter> buffetEmitters = emitters.get(buffetId);
        if (buffetEmitters == null) {
            return;
        }

        buffetEmitters.remove(emitter);
        if (buffetEmitters.isEmpty()) {
            emitters.remove(buffetId);
        }
    }
}
