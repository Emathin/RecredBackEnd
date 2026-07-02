package com.recred.backend.unit.application;

import com.recred.backend.application.*;

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

        ProductResponse producto1 = productResponse(UUID.randomUUID(), "Producto 1");
        ProductResponse producto2 = productResponse(UUID.randomUUID(), "Producto 2");

        when(saveProduct.execute(command1)).thenReturn(producto1);
        when(saveProduct.execute(command2)).thenReturn(producto2);

        SaveBulkProductsResponse resultado =
                saveBulkProducts.execute(new SaveBulkProductsRequest(List.of(command1, command2)));

        assertEquals(2, resultado.products().size());
        assertEquals("Producto 1", resultado.products().get(0).nombre());
        assertEquals("Producto 2", resultado.products().get(1).nombre());

        verify(saveProduct, times(1)).execute(command1);
        verify(saveProduct, times(1)).execute(command2);
    }

    private ProductResponse productResponse(UUID id, String name) {
        return new ProductResponse(
                id,
                name,
                null,
                null,
                null,
                false,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                java.util.Set.of(),
                java.util.Set.of()
        );
    }
}
