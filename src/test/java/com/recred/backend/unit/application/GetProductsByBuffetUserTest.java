package com.recred.backend.unit.application;

import com.recred.backend.application.GetProductsByBuffetUser;
import com.recred.backend.application.ProductResponse;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetProductsByBuffetUserTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private KiosqueroRepository kiosqueroRepository;

    @InjectMocks
    private GetProductsByBuffetUser getProductsByBuffetUser;

    @Test
    void execute_cuandoUserIdNoEsNulo_deberiaRetornarProductosActivosConStockDeSuBuffet() {
        UUID userId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());

        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(buffetId);
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(producto));

        List<ProductResponse> resultado = getProductsByBuffetUser.execute(userId);

        assertEquals(1, resultado.size());
        assertEquals(producto.getId(), resultado.get(0).id());
        verify(kiosqueroRepository, times(1)).findBuffetByUsuarioId(userId);
        verify(productRepository, times(1)).findAllActiveWithStock(buffetId);
    }

    @Test
    void execute_cuandoUserIdEsNulo_deberiaRetornarTodosLosProductosActivos() {
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());

        when(productRepository.findAllActive()).thenReturn(List.of(producto));

        List<ProductResponse> resultado = getProductsByBuffetUser.execute(null);

        assertEquals(1, resultado.size());
        assertEquals(producto.getId(), resultado.get(0).id());
        verifyNoInteractions(kiosqueroRepository);
        verify(productRepository, times(1)).findAllActive();
    }
}
