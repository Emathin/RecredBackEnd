package com.recred.backend.application;

import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveProductTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private SaveProduct saveProduct;

    @Test
    void execute_GuardaProductoEInventario_CuandoVienenDatosCompletos() {
        UUID categoryId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        SaveProductCommand command = new SaveProductCommand(
                "Galletitas",
                "Deliciosas",
                new BigDecimal("150.00"),
                new BigDecimal("0.5"),
                false,
                categoryId,
                buffetId,
                50,
                Set.of(UUID.randomUUID()),
                Set.of(UUID.randomUUID())
        );

        Producto savedProduct = new Producto();
        savedProduct.setId(UUID.randomUUID());
        when(productRepository.save(any(Producto.class))).thenReturn(savedProduct);

        Producto result = saveProduct.execute(command);

        assertNotNull(result);
        assertEquals(50, result.getStockActual());
        verify(productRepository).save(any(Producto.class));
        verify(inventoryRepository).save(any(Inventario.class));
    }
}
