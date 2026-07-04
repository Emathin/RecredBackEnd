package com.recred.backend.unit.application;

import com.recred.backend.application.UpdateProductPrice;
import com.recred.backend.application.UpdateProductPriceRequest;
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

import static java.util.UUID.randomUUID;
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
        UUID productId = randomUUID();
        UUID buffetId = randomUUID();
        Producto product = product(productId, "100.00");
        BigDecimal newPrice = new BigDecimal("120.00");
        givenProductExists(productId, product);

        whenUpdatingPrice(productId, newPrice, buffetId);

        verify(productRepository).updatePrice(productId, newPrice);

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
        UUID productId = randomUUID();
        Producto product = product(productId, "100.00");
        givenProductExists(productId, product);

        whenUpdatingPrice(productId, new BigDecimal("100.00"), null);

        verify(productRepository, never()).updatePrice(any(), any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void updatePriceDoesNotRequireProductCategory() {
        UUID productId = randomUUID();
        Producto product = product(productId, "100.00");
        product.setCategoria(null);
        BigDecimal newPrice = new BigDecimal("120.00");
        givenProductExists(productId, product);

        whenUpdatingPrice(productId, newPrice, null);

        verify(productRepository).updatePrice(productId, newPrice);
    }

    @Test
    void rejectZeroPrice() {
        UUID productId = randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> whenUpdatingPrice(productId, BigDecimal.ZERO, null));
    }

    @Test
    void rejectNullPrice() {
        UUID productId = randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> whenUpdatingPrice(productId, null, null));
    }

    private UpdateProductPriceRequest request(
            UUID productId,
            BigDecimal newPrice,
            UUID buffetId
    ) {
        return new UpdateProductPriceRequest(productId, newPrice, buffetId);
    }

    private void whenUpdatingPrice(UUID productId, BigDecimal newPrice, UUID buffetId) {
        updateProductPrice.execute(request(productId, newPrice, buffetId));
    }

    private void givenProductExists(UUID productId, Producto product) {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    }

    private Producto product(UUID productId, String price) {
        Producto product = new Producto();
        product.setId(productId);
        product.setPrecio(new BigDecimal(price));
        return product;
    }
}
