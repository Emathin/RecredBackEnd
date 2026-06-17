package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SaveBulkProductsTest {

    @Mock
    private SaveProduct saveProduct;

    @InjectMocks
    private SaveBulkProducts saveBulkProducts;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_debeLlamarASaveProductPorCadaComandoYRetornarListaDeProductos() {
        SaveProductCommand command1 = new SaveProductCommand(
                "Producto 1", "Desc 1", new BigDecimal("100"), new BigDecimal("500"), false,
                UUID.randomUUID(), null, UUID.randomUUID(), 10,
                null, null
        );
        SaveProductCommand command2 = new SaveProductCommand(
                "Producto 2", "Desc 2", new BigDecimal("200"), new BigDecimal("200"), false,
                UUID.randomUUID(), null, UUID.randomUUID(), 5,
                null, null
        );

        Producto producto1 = new Producto();
        producto1.setId(UUID.randomUUID());
        producto1.setNombre("Producto 1");

        Producto producto2 = new Producto();
        producto2.setId(UUID.randomUUID());
        producto2.setNombre("Producto 2");

        when(saveProduct.execute(command1)).thenReturn(producto1);
        when(saveProduct.execute(command2)).thenReturn(producto2);

        List<Producto> resultado = saveBulkProducts.execute(List.of(command1, command2));

        assertEquals(2, resultado.size());
        assertEquals("Producto 1", resultado.get(0).getNombre());
        assertEquals("Producto 2", resultado.get(1).getNombre());

        verify(saveProduct, times(1)).execute(command1);
        verify(saveProduct, times(1)).execute(command2);
    }
}
