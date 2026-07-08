package com.recred.backend.unit.application;

import com.recred.backend.application.GetProductById;
import com.recred.backend.application.GetProductByIdRequest;
import com.recred.backend.application.ProductResponse;
import com.recred.backend.domain.exceptions.ProductNotFoundException;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProductByIdTest {

    @Mock
    private ProductRepository productRepository;

    private GetProductById useCase;
    private UUID productId;
    private UUID buffetId;

    @BeforeEach
    void setUp() {
        useCase = new GetProductById(productRepository);
    }

    @Test
    void givenProductExists_whenGettingProductWithoutBuffet_thenReturnsProduct() {
        givenProductExists();

        final ProductResponse response = whenGetProductWithoutBuffet();

        thenProductWasReturned(response);
        thenProductWasRequestedWithoutBuffet();
    }

    @Test
    void givenProductDoesNotExist_whenGettingProductWithoutBuffet_thenThrowsException() {
        givenProductDoesNotExist();

        final ProductNotFoundException exception = whenGetProductWithoutBuffetFails();

        thenProductNotFoundWasThrown(exception);
        thenProductWasRequestedWithoutBuffet();
    }

    @Test
    void givenProductExistsForBuffet_whenGettingProductWithBuffet_thenReturnsProductWithStock() {
        givenProductExistsForBuffet();

        final ProductResponse response = whenGetProductWithBuffet();

        thenProductWasReturned(response);
        thenProductWasRequestedWithBuffet();
    }

    @Test
    void givenProductDoesNotExistForBuffet_whenGettingProductWithBuffet_thenThrowsException() {
        givenProductDoesNotExistForBuffet();

        final ProductNotFoundException exception = whenGetProductWithBuffetFails();

        thenProductNotFoundWasThrown(exception);
        thenProductWasRequestedWithBuffet();
    }

    private void givenProductExists() {
        productId = randomUUID();
        when(productRepository.findActiveById(productId))
                .thenReturn(Optional.of(product(productId)));
    }

    private void givenProductDoesNotExist() {
        productId = randomUUID();
        when(productRepository.findActiveById(productId)).thenReturn(Optional.empty());
    }

    private void givenProductExistsForBuffet() {
        productId = randomUUID();
        buffetId = randomUUID();
        when(productRepository.findActiveByIdWithStock(productId, buffetId))
                .thenReturn(Optional.of(product(productId)));
    }

    private void givenProductDoesNotExistForBuffet() {
        productId = randomUUID();
        buffetId = randomUUID();
        when(productRepository.findActiveByIdWithStock(productId, buffetId))
                .thenReturn(Optional.empty());
    }

    private ProductResponse whenGetProductWithoutBuffet() {
        return useCase.execute(new GetProductByIdRequest(productId, null));
    }

    private ProductResponse whenGetProductWithBuffet() {
        return useCase.execute(new GetProductByIdRequest(productId, buffetId));
    }

    private ProductNotFoundException whenGetProductWithoutBuffetFails() {
        return assertThrows(ProductNotFoundException.class, executeWithoutBuffet());
    }

    private ProductNotFoundException whenGetProductWithBuffetFails() {
        return assertThrows(ProductNotFoundException.class, executeWithBuffet());
    }

    private void thenProductWasReturned(ProductResponse response) {
        assertEquals(productId, response.id());
    }

    private void thenProductNotFoundWasThrown(ProductNotFoundException exception) {
        assertEquals(ProductNotFoundException.class, exception.getClass());
    }

    private void thenProductWasRequestedWithoutBuffet() {
        verify(productRepository).findActiveById(productId);
    }

    private void thenProductWasRequestedWithBuffet() {
        verify(productRepository).findActiveByIdWithStock(productId, buffetId);
    }

    private Executable executeWithoutBuffet() {
        return () -> useCase.execute(new GetProductByIdRequest(productId, null));
    }

    private Executable executeWithBuffet() {
        return () -> useCase.execute(new GetProductByIdRequest(productId, buffetId));
    }

    private Producto product(UUID requestedProductId) {
        Producto product = new Producto();
        product.setId(requestedProductId);
        return product;
    }
}
