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
    void dadoQueElBuffetIdEsNuloYExisteProducto_cuandoExecute_entoncesRetornaProducto() {
        UUID id = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(id);
        when(productRepository.findActiveById(id)).thenReturn(Optional.of(producto));

        ProductResponse result = getProductById.execute(new GetProductByIdRequest(id, null));

        assertEquals(id, result.id());
        verify(productRepository).findActiveById(id);
    }

    @Test
    void dadoQueElBuffetIdEsNuloYNoExisteProducto_cuandoExecute_entoncesLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        when(productRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> getProductById.execute(new GetProductByIdRequest(id, null)));
        verify(productRepository).findActiveById(id);
    }

    @Test
    void dadoQueElBuffetIdEsValidoYExisteProducto_cuandoExecute_entoncesRetornaProductoConStock() {
        UUID id = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(id);
        when(productRepository.findActiveByIdWithStock(id, buffetId)).thenReturn(Optional.of(producto));

        ProductResponse result = getProductById.execute(
                new GetProductByIdRequest(id, buffetId));

        assertEquals(id, result.id());
        verify(productRepository).findActiveByIdWithStock(id, buffetId);
    }

    @Test
    void dadoQueElBuffetIdEsValidoYNoExisteProducto_cuandoExecute_entoncesLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        when(productRepository.findActiveByIdWithStock(id, buffetId)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> getProductById.execute(new GetProductByIdRequest(id, buffetId)));
        verify(productRepository).findActiveByIdWithStock(id, buffetId);
    }
}
