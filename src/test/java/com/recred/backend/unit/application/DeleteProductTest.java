package com.recred.backend.unit.application;

import com.recred.backend.application.DeleteProduct;
import com.recred.backend.application.DeleteProductRequest;
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
        DeleteProductRequest request = new DeleteProductRequest(id);

        when(productRepository.findActiveById(id)).thenReturn(Optional.of(producto));

        deleteProduct.execute(request);

        assertFalse(producto.getActivo());
        verify(productRepository).save(producto);
    }

    @Test
    void dadoQueNoExisteProducto_cuandoExecute_entoncesLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        DeleteProductRequest request = new DeleteProductRequest(id);

        when(productRepository.findActiveById(id)).thenReturn(Optional.empty());

        Executable execution = () -> deleteProduct.execute(request);

        assertThrows(ProductNotFoundException.class, execution);
        verify(productRepository, never()).save(any());
    }
}
