package com.recred.backend.application;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.repositories.CategoryRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveProductTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private CategoryRepository categoryRepository;

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
                null,
                buffetId,
                50,
                Set.of(UUID.randomUUID()),
                Set.of(UUID.randomUUID())
        );

        Producto savedProduct = new Producto();
        savedProduct.setId(UUID.randomUUID());
        when(productRepository.save(any(Producto.class))).thenReturn(savedProduct);
        when(inventoryRepository.findByBuffetAndProduct(any(), any())).thenReturn(java.util.Optional.empty());

        Producto result = saveProduct.execute(command);

        assertNotNull(result);
        assertEquals(50, result.getStockActual());
        verify(productRepository).save(any(Producto.class));
        verify(inventoryRepository).save(any(Inventario.class));
    }

    @Test
    void execute_CreaNuevaCategoria_CuandoNoExiste() {
        SaveProductCommand command = new SaveProductCommand(
                "Producto", "Desc", BigDecimal.TEN, BigDecimal.ONE, false,
                null, "Nueva Cat", null, null, null, null
        );

        when(categoryRepository.findByDescription("Nueva Cat")).thenReturn(java.util.Optional.empty());
        CategoriaProducto newCat = new CategoriaProducto();
        newCat.setDescripcion("Nueva Cat");
        when(categoryRepository.save(any(CategoriaProducto.class))).thenReturn(newCat);
        when(productRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        Producto result = saveProduct.execute(command);

        assertNotNull(result.getCategoria());
        assertEquals("Nueva Cat", result.getCategoria().getDescripcion());
        verify(categoryRepository).save(any(CategoriaProducto.class));
    }

    @Test
    void execute_UsaCategoriaExistente_CuandoSeProveeNombre() {
        SaveProductCommand command = new SaveProductCommand(
                "Producto", "Desc", BigDecimal.TEN, BigDecimal.ONE, false,
                null, "Cat Existente", null, null, null, null
        );

        CategoriaProducto existingCat = new CategoriaProducto();
        existingCat.setDescripcion("Cat Existente");
        when(categoryRepository.findByDescription("Cat Existente")).thenReturn(java.util.Optional.of(existingCat));
        when(productRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        Producto result = saveProduct.execute(command);

        assertEquals(existingCat, result.getCategoria());
    }

    @Test
    void execute_ActualizaInventarioExistente_CuandoYaExiste() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        SaveProductCommand command = new SaveProductCommand(
                "Prod", "Desc", BigDecimal.TEN, BigDecimal.ONE, false,
                null, null, buffetId, 100, null, null
        );

        Producto savedProduct = new Producto();
        savedProduct.setId(productId);
        when(productRepository.save(any(Producto.class))).thenReturn(savedProduct);

        Inventario existingInventory = new Inventario();
        when(inventoryRepository.findByBuffetAndProduct(buffetId, productId))
                .thenReturn(java.util.Optional.of(existingInventory));

        saveProduct.execute(command);

        assertEquals(100, existingInventory.getStockActual());
        verify(inventoryRepository).save(existingInventory);
    }

    @Test
    void execute_GuardaInventarioSinStockActual_CuandoVieneTipoManejo() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        SaveProductCommand command = new SaveProductCommand(
                "Prod",
                "Desc",
                BigDecimal.TEN,
                BigDecimal.ONE,
                false,
                null,
                null,
                buffetId,
                null,
                null,
                null,
                TipoManejoInventario.CUPO_DIARIO,
                20
        );
        Producto savedProduct = new Producto();
        savedProduct.setId(productId);
        when(productRepository.save(any(Producto.class))).thenReturn(savedProduct);
        when(inventoryRepository.findByBuffetAndProduct(buffetId, productId))
                .thenReturn(java.util.Optional.empty());

        Producto result = saveProduct.execute(command);

        assertNull(result.getStockActual());
        assertNull(result.getStockDisponible());
        assertEquals(TipoManejoInventario.CUPO_DIARIO, result.getTipoManejoInventario());
        assertEquals(20, result.getCupoMaximoDiario());
        verify(inventoryRepository).save(any(Inventario.class));
    }

    @Test
    void execute_MapeaClasificacionesYTipos_CuandoSeProveen() {
        UUID saludId = UUID.randomUUID();
        UUID tipoId = UUID.randomUUID();
        SaveProductCommand command = new SaveProductCommand(
                "Prod", null, null, null, true,
                null, null, null, null, Set.of(saludId), Set.of(tipoId)
        );

        when(productRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        Producto result = saveProduct.execute(command);

        assertEquals(1, result.getClasificacionesSalud().size());
        assertEquals(saludId, result.getClasificacionesSalud().iterator().next().getId());
        assertEquals(1, result.getTipos().size());
        assertEquals(tipoId, result.getTipos().iterator().next().getId());
    }

    @Test
    void execute_NoAsignaCategoria_CuandoAmbosCamposSonNulos() {
        SaveProductCommand command = new SaveProductCommand(
                "Prod", null, null, null, true,
                null, "", null, null, null, null
        );

        when(productRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        Producto result = saveProduct.execute(command);

        assertNull(result.getCategoria());
    }

    @Test
    void execute_NoGuardaInventario_CuandoStockEsNulo() {
        UUID buffetId = UUID.randomUUID();
        SaveProductCommand command = new SaveProductCommand(
                "Prod", null, null, null, true,
                null, null, buffetId, null, null, null
        );

        when(productRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        saveProduct.execute(command);

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void execute_NoGuardaInventario_CuandoBuffetIdEsNulo() {
        SaveProductCommand command = new SaveProductCommand(
                "Prod", null, null, null, true,
                null, null, null, 100, null, null
        );

        when(productRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        saveProduct.execute(command);

        verify(inventoryRepository, never()).save(any());
    }
}
