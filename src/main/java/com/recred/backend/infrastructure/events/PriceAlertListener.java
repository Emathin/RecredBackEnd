package com.recred.backend.infrastructure.events;

import com.recred.backend.application.GeneratePriceAlert;
import com.recred.backend.domain.events.ProductPriceChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PriceAlertListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PriceAlertListener.class);

    private final GeneratePriceAlert generatePriceAlert;

    public PriceAlertListener(GeneratePriceAlert generatePriceAlert) {
        this.generatePriceAlert = generatePriceAlert;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductPriceChanged(ProductPriceChangedEvent event) {
        LOGGER.info(
                "price_alert_event_received productId={} oldPrice={} newPrice={}",
                event.product().getId(),
                event.oldPrice(),
                event.newPrice()
        );
        generatePriceAlert.execute(
                event.product(),
                event.oldPrice(),
                event.newPrice(),
                event.buffetId()
        );
    }
}
