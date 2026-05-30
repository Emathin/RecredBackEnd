package com.recred.backend.infrastructure.events;

import com.recred.backend.domain.events.ProductPriceChangedEvent;
import com.recred.backend.domain.events.ProductPriceChangedPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringProductPriceChangedPublisher implements ProductPriceChangedPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public SpringProductPriceChangedPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void publish(ProductPriceChangedEvent event) {
        eventPublisher.publishEvent(event);
    }
}
