package com.recred.backend.infrastructure.events;

import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimeEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringKiosqueroRealtimeEventPublisher implements KiosqueroRealtimeEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public SpringKiosqueroRealtimeEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void publish(KiosqueroRealtimeEvent event) {
        eventPublisher.publishEvent(event);
    }
}
