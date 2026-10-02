package com.recred.backend.unit.infrastructure.events;

import com.recred.backend.infrastructure.events.*;

import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimeEventType;
import com.recred.backend.domain.events.ProductPriceChangedEvent;
import com.recred.backend.domain.models.Producto;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SpringEventPublisherTest {

    @Test
    void productPriceChangedPublisherShouldPublishSpringEvent() {
        ApplicationEventPublisher applicationEventPublisher =
                mock(ApplicationEventPublisher.class);
        SpringProductPriceChangedPublisher publisher =
                new SpringProductPriceChangedPublisher(applicationEventPublisher);
        ProductPriceChangedEvent event = new ProductPriceChangedEvent(
                new Producto(),
                BigDecimal.ONE,
                BigDecimal.TEN,
                UUID.randomUUID()
        );

        publisher.publish(event);

        verify(applicationEventPublisher).publishEvent(event);
    }

    @Test
    void kiosqueroRealtimeEventPublisherShouldPublishSpringEvent() {
        ApplicationEventPublisher applicationEventPublisher =
                mock(ApplicationEventPublisher.class);
        SpringKiosqueroRealtimeEventPublisher publisher =
                new SpringKiosqueroRealtimeEventPublisher(applicationEventPublisher);
        KiosqueroRealtimeEvent event = KiosqueroRealtimeEvent.productAlert(
                UUID.randomUUID(),
                KiosqueroRealtimeEventType.STOCK_CHANGED,
                UUID.randomUUID(),
                "Actualizado"
        );

        publisher.publish(event);

        verify(applicationEventPublisher).publishEvent(event);
    }
}
