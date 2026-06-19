package com.recred.backend.infrastructure.events;

import com.recred.backend.application.GeneratePriceAlert;
import com.recred.backend.domain.events.ProductPriceChangedEvent;
import com.recred.backend.domain.models.Producto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PriceAlertListenerTest {

    @Mock
    private GeneratePriceAlert generatePriceAlert;

    @Test
    void handleProductPriceChanged_ShouldDelegateToGeneratePriceAlert() {
        PriceAlertListener listener = new PriceAlertListener(generatePriceAlert);
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Producto product = new Producto();
        product.setId(productId);
        BigDecimal oldPrice = new BigDecimal("100.00");
        BigDecimal newPrice = new BigDecimal("120.00");

        listener.handleProductPriceChanged(new ProductPriceChangedEvent(
                product,
                oldPrice,
                newPrice,
                buffetId
        ));

        verify(generatePriceAlert).execute(product, oldPrice, newPrice, buffetId);
    }
}
