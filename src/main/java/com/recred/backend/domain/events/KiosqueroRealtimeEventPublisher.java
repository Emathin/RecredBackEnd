package com.recred.backend.domain.events;

public interface KiosqueroRealtimeEventPublisher {
    void publish(KiosqueroRealtimeEvent event);
}
