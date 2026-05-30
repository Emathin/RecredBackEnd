package com.recred.backend.application;

import com.recred.backend.domain.events.ProductPriceChangedEvent;
import com.recred.backend.domain.events.ProductPriceChangedPublisher;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateProductPriceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductPriceChangedPublisher eventPublisher;

    private UpdateProductPrice updateProductPrice;

    @BeforeEach
    void setUp() {
        updateProductPrice = new UpdateProductPrice(productRepository, eventPublisher);
    }

    @Test
    void updatePriceAndPublishEventWhenPriceChanges() {
        UUID productId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Producto product = product(productId, "100.00");
        BigDecimal newPrice = new BigDecimal("120.00");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        updateProductPrice.execute(productId, newPrice, buffetId);

        verify(productRepository).save(product);
        ArgumentCaptor<ProductPriceChangedEvent> captor =
                ArgumentCaptor.forClass(ProductPriceChangedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        ProductPriceChangedEvent event = captor.getValue();
        assertEquals(product, event.producto());
        assertEquals(new BigDecimal("100.00"), event.oldPrice());
        assertEquals(newPrice, event.newPrice());
        assertEquals(buffetId, event.buffetId());
    }

    @Test
    void doNothingWhenPriceDoesNotChange() {
        UUID productId = UUID.randomUUID();
        Producto product = product(productId, "100.00");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        updateProductPrice.execute(productId, new BigDecimal("100.00"), null);

        verify(productRepository, never()).save(product);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void rejectNullOrNonPositivePrices() {
        UUID productId = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () -> updateProductPrice.execute(productId, BigDecimal.ZERO, null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> updateProductPrice.execute(productId, null, null)
        );
    }

    private Producto product(UUID productId, String price) {
        Producto product = new Producto();
        product.setId(productId);
        product.setPrecio(new BigDecimal(price));
        return product;
    }
}
