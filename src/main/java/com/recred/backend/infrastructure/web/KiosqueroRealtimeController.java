package com.recred.backend.infrastructure.web;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/kiosqueros")
public class KiosqueroRealtimeController {

    private final KiosqueroRealtimeEventStream eventStream;
    private final KiosqueroAccessGuard kiosqueroAccessGuard;

    public KiosqueroRealtimeController(
            KiosqueroRealtimeEventStream eventStream,
            KiosqueroAccessGuard kiosqueroAccessGuard
    ) {
        this.eventStream = eventStream;
        this.kiosqueroAccessGuard = kiosqueroAccessGuard;
    }

    @GetMapping(value = "/{buffetId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable UUID buffetId, Authentication authentication) {
        kiosqueroAccessGuard.requireBuffetAccess(authentication, buffetId);
        return eventStream.subscribe(buffetId);
    }
}
