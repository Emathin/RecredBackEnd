package com.recred.backend.unit.application;

import com.recred.backend.application.GetProductById;
import com.recred.backend.domain.exceptions.ProductNotFoundException;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProductByIdTest {

    @Mock
    private ProductRepository productRepository;

    private GetProductById getProductById;

    @BeforeEach
    void setUp() {
        getProductById = new GetProductById(productRepository);
    }

    @Test
    void executeWhenBuffetIdIsNullAndProductExistsShouldReturnProduct() {
        UUID id = UUID.randomUUID();
        Producto product = new Producto();
        product.setId(id);
        when(productRepository.findActiveById(id)).thenReturn(Optional.of(product));

        Producto result = getProductById.execute(id, null);

        assertEquals(id, result.getId());
        verify(productRepository).findActiveById(id);
    }

    @Test
    void executeWhenBuffetIdIsNullAndProductDoesNotExistShouldThrowException() {
        UUID id = UUID.randomUUID();
        when(productRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> getProductById.execute(id, null));
        verify(productRepository).findActiveById(id);
    }

    @Test
    void executeWhenBuffetIdIsValidAndProductExistsShouldReturnProductWithStock() {
        UUID id = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Producto product = new Producto();
        product.setId(id);
        when(productRepository.findActiveByIdWithStock(id, buffetId)).thenReturn(Optional.of(product));

        Producto result = getProductById.execute(id, buffetId);

        assertEquals(id, result.getId());
        verify(productRepository).findActiveByIdWithStock(id, buffetId);
    }

    @Test
    void executeWhenBuffetIdIsValidAndProductDoesNotExistShouldThrowException() {
        UUID id = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        when(productRepository.findActiveByIdWithStock(id, buffetId)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> getProductById.execute(id, buffetId));
        verify(productRepository).findActiveByIdWithStock(id, buffetId);
    }
}
