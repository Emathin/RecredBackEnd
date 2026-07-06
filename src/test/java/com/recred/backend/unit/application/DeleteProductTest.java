package com.recred.backend.unit.application;

import com.recred.backend.application.DeleteProduct;
import com.recred.backend.application.DeleteProductRequest;
import com.recred.backend.domain.exceptions.ProductNotFoundException;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteProductTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final DeleteProduct useCase = new DeleteProduct(productRepository);

    @Test
    void deactivateProductSuccessfully() {
        Producto product = new Producto();
        product.setId(PRODUCT_ID);
        product.setActivo(true);
        
        givenAnActiveProduct(product);

        whenExecuteUseCase();

        thenProductShouldBeDeactivatedAndSaved(product);
    }

    @Test
    void throwExceptionWhenProductNotFound() {
        givenProductNotFound();

        assertThrows(ProductNotFoundException.class, this::whenExecuteUseCase);
        
        thenProductShouldNotBeSaved();
    }

    private void givenAnActiveProduct(Producto product) {
        when(productRepository.findActiveById(PRODUCT_ID)).thenReturn(Optional.of(product));
    }

    private void givenProductNotFound() {
        when(productRepository.findActiveById(PRODUCT_ID)).thenReturn(Optional.empty());
    }

    private void whenExecuteUseCase() {
        useCase.execute(new DeleteProductRequest(PRODUCT_ID));
    }

    private void thenProductShouldBeDeactivatedAndSaved(Producto product) {
        assertFalse(product.getActivo());
        verify(productRepository).save(product);
    }

    private void thenProductShouldNotBeSaved() {
        verify(productRepository, never()).save(any());
    }
}
