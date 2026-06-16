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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    void dadoQueExisteProducto_cuandoExecute_entoncesDesactivaYGuarda() {
        UUID id = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(id);
        producto.setActivo(true);
        when(productRepository.findActiveById(id)).thenReturn(Optional.of(producto));

        deleteProduct.execute(id);

        assertFalse(producto.getActivo());
        verify(productRepository).save(producto);
    }

    @Test
    void dadoQueNoExisteProducto_cuandoExecute_entoncesLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        when(productRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> deleteProduct.execute(id));
        verify(productRepository, never()).save(any());
    }
}
