package com.recred.backend.domain.events;

public interface ProductPriceChangedPublisher {

    void publish(ProductPriceChangedEvent event);
}
