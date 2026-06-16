package com.recred.backend.unit.application;

import com.recred.backend.application.UpdateProduct;
import com.recred.backend.application.UpdateProductCommand;
import com.recred.backend.domain.exceptions.ProductNotFoundException;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.repositories.CategoryRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateProductTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private CategoryRepository categoryRepository;

    private UpdateProduct updateProduct;

    @BeforeEach
    void setUp() {
        updateProduct = new UpdateProduct(productRepository, inventoryRepository, categoryRepository);
    }

    @Test
    void dadoQueNoExisteProducto_cuandoExecute_entoncesLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        UpdateProductCommand command = new UpdateProductCommand(
                id, "Nombre", "Desc", BigDecimal.TEN, new BigDecimal("0.5"), false, 
                null, null, null, null, null, null
        );

        when(productRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> updateProduct.execute(command));
    }

    @Test
    void dadoQueExisteProductoYComandoValido_cuandoExecute_entoncesActualizaYGuarda() {
        UUID id = UUID.randomUUID();
        UpdateProductCommand command = new UpdateProductCommand(
                id, "Nombre Act", "Desc Act", BigDecimal.TEN, new BigDecimal("0.5"), false, 
                null, null, null, null, null, null
        );

        Producto existing = new Producto();
        existing.setId(id);
        
        when(productRepository.findActiveById(id)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        Producto result = updateProduct.execute(command);

        assertEquals("Nombre Act", existing.getNombre());
        assertEquals("Desc Act", existing.getDescripcion());
        verify(productRepository).save(existing);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void dadoQueVieneDataDeInventario_cuandoExecute_entoncesActualizaStock() {
        UUID id = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UpdateProductCommand command = new UpdateProductCommand(
                id, "Nombre Act", "Desc Act", BigDecimal.TEN, new BigDecimal("0.5"), false, 
                null, null, buffetId, 100, null, null
        );

        Producto existing = new Producto();
        existing.setId(id);

        Inventario inventario = new Inventario();
        inventario.setStockActual(10);
        
        when(productRepository.findActiveById(id)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);
        when(inventoryRepository.findByBuffetAndProduct(buffetId, id)).thenReturn(Optional.of(inventario));

        updateProduct.execute(command);

        assertEquals(100, inventario.getStockActual());
        verify(inventoryRepository).save(inventario);
    }
}
