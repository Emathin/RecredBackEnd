package com.recred.backend.unit.infrastructure.events;

import com.recred.backend.application.GeneratePriceAlert;
import com.recred.backend.application.GeneratePriceAlertRequest;
import com.recred.backend.domain.events.ProductPriceChangedEvent;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.infrastructure.events.PriceAlertListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PriceAlertListenerTest {

    private static final BigDecimal OLD_PRICE = new BigDecimal("100.00");
    private static final BigDecimal NEW_PRICE = new BigDecimal("120.00");

    @Mock
    private GeneratePriceAlert generatePriceAlert;

    @Test
    void handleProductPriceChanged_ShouldDelegateToGeneratePriceAlert() {
        PriceAlertListener listener = new PriceAlertListener(generatePriceAlert);
        Producto product = product();
        UUID buffetId = UUID.randomUUID();

        listener.handleProductPriceChanged(priceChangedEvent(product, buffetId));

        GeneratePriceAlertRequest request = capturedRequest();
        assertDelegatedRequest(request, product, buffetId);
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        return product;
    }

    private ProductPriceChangedEvent priceChangedEvent(Producto product, UUID buffetId) {
        return new ProductPriceChangedEvent(
                product,
                OLD_PRICE,
                NEW_PRICE,
                buffetId
        );
    }

    private void assertDelegatedRequest(
            GeneratePriceAlertRequest request,
            Producto product,
            UUID buffetId
    ) {
        assertSame(product, request.product());
        assertEquals(OLD_PRICE, request.oldPrice());
        assertEquals(NEW_PRICE, request.newPrice());
        assertEquals(buffetId, request.buffetId());
    }

    private GeneratePriceAlertRequest capturedRequest() {
        ArgumentCaptor<GeneratePriceAlertRequest> captor =
                ArgumentCaptor.forClass(GeneratePriceAlertRequest.class);
        verify(generatePriceAlert).execute(captor.capture());
        return captor.getValue();
    }
}
