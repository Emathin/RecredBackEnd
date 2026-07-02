package com.recred.backend.unit.application;

import com.recred.backend.application.GetAllProducts;
import com.recred.backend.application.ProductResponse;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAllProductsTest {

    @Mock
    private ProductRepository productRepository;

    private GetAllProducts getAllProducts;

    @BeforeEach
    void setUp() {
        getAllProducts = new GetAllProducts(productRepository);
    }

    @Test
    void dadoQueElBuffetIdEsNulo_cuandoExecute_entoncesLlamaBusquedaGlobal() {
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        when(productRepository.findAllActive()).thenReturn(List.of(producto));

        List<ProductResponse> result = getAllProducts.execute(null);

        assertEquals(1, result.size());
        verify(productRepository).findAllActive();
    }

    @Test
    void dadoQueElBuffetIdEsValido_cuandoExecute_entoncesLlamaBusquedaConStock() {
        UUID buffetId = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(producto));

        List<ProductResponse> result = getAllProducts.execute(buffetId);

        assertEquals(1, result.size());
        verify(productRepository).findAllActiveWithStock(buffetId);
    }
}
