package com.recred.backend.unit.application;

import com.recred.backend.application.DeleteProduct;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteProductTest {

    @Mock
    private ProductRepository productRepository;

    private DeleteProduct deleteProduct;

    @BeforeEach
    void setUp() {
        deleteProduct = new DeleteProduct(productRepository);
    }

    @Test
    void executeWhenProductExistsShouldDeactivateAndSave() {
        UUID id = UUID.randomUUID();
        Producto product = new Producto();
        product.setId(id);
        product.setActivo(true);
        when(productRepository.findActiveById(id)).thenReturn(Optional.of(product));

        deleteProduct.execute(id);

        assertFalse(product.getActivo());
        verify(productRepository).save(product);
    }

    @Test
    void executeWhenProductDoesNotExistShouldThrowException() {
        UUID id = UUID.randomUUID();
        when(productRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> deleteProduct.execute(id));
        verify(productRepository, never()).save(any());
    }
}
