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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateProductPriceTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID BUFFET_ID = UUID.randomUUID();
    private static final BigDecimal OLD_PRICE = new BigDecimal("100.00");
    private static final BigDecimal NEW_PRICE = new BigDecimal("120.00");

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
    void shouldUpdatePriceAndPublishEventWhenPriceChanges() {
        givenProductExistsWithPrice(OLD_PRICE);

        whenUpdatingPrice(NEW_PRICE, BUFFET_ID);

        thenPriceWasUpdatedInRepository(NEW_PRICE);
        thenPriceChangedEventWasPublished(NEW_PRICE);
    }

    @Test
    void shouldDoNothingWhenPriceDoesNotChange() {
        givenProductExistsWithPrice(OLD_PRICE);

        whenUpdatingPrice(OLD_PRICE, null);

        thenNoActionWasTaken();
    }

    @Test
    void shouldUpdatePriceWhenProductHasNoCategory() {
        givenProductExistsWithoutCategory(OLD_PRICE);

        whenUpdatingPrice(NEW_PRICE, null);

        thenPriceWasUpdatedInRepository(NEW_PRICE);
    }

    private void givenProductExistsWithPrice(BigDecimal price) {
        Producto product = new Producto();
        product.setId(PRODUCT_ID);
        product.setPrecio(price);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
    }

    private void givenProductExistsWithoutCategory(BigDecimal price) {
        Producto product = new Producto();
        product.setId(PRODUCT_ID);
        product.setPrecio(price);
        product.setCategoria(null);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
    }

    private void whenUpdatingPrice(BigDecimal newPrice, UUID buffetId) {
        updateProductPrice.execute(new UpdateProductPriceRequest(PRODUCT_ID, newPrice, buffetId));
    }

    private void thenPriceWasUpdatedInRepository(BigDecimal newPrice) {
        verify(productRepository).updatePrice(PRODUCT_ID, newPrice);
    }

    private void thenPriceChangedEventWasPublished(BigDecimal newPrice) {
        ArgumentCaptor<ProductPriceChangedEvent> captor = 
                ArgumentCaptor.forClass(ProductPriceChangedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        ProductPriceChangedEvent event = captor.getValue();
        
        assertEquals(PRODUCT_ID, event.producto().getId());
        assertEquals(OLD_PRICE, event.oldPrice());
        assertEquals(newPrice, event.newPrice());
        assertEquals(BUFFET_ID, event.buffetId());
    }

    private void thenNoActionWasTaken() {
        verify(productRepository, never()).updatePrice(any(), any());
        verify(eventPublisher, never()).publish(any());
    }
}
