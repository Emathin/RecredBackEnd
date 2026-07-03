package com.recred.backend.unit.application;

import com.recred.backend.application.SaveProduct;
import com.recred.backend.application.SaveProductRequest;
import com.recred.backend.application.ProductResponse;
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
        SaveProductRequest command = new SaveProductRequest(
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

        ProductResponse result = saveProduct.execute(command);

        assertNotNull(result);
        assertEquals(50, result.stockActual());
        verify(productRepository).save(any(Producto.class));
        verify(inventoryRepository).save(any(Inventario.class));
    }

    @Test
    void execute_CreaNuevaCategoria_CuandoNoExiste() {
        SaveProductRequest command = new SaveProductRequest(
                "Producto", "Desc", BigDecimal.TEN, BigDecimal.ONE, false,
                null, "Nueva Cat", null, null, null, null
        );

        when(categoryRepository.findByDescription("Nueva Cat")).thenReturn(java.util.Optional.empty());
        CategoriaProducto newCat = new CategoriaProducto();
        newCat.setDescripcion("Nueva Cat");
        when(categoryRepository.save(any(CategoriaProducto.class))).thenReturn(newCat);
        when(productRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        ProductResponse result = saveProduct.execute(command);

        assertNotNull(result.categoria());
        assertEquals("Nueva Cat", result.categoria().descripcion());
        verify(categoryRepository).save(any(CategoriaProducto.class));
    }

    @Test
    void execute_UsaCategoriaExistente_CuandoSeProveeNombre() {
        SaveProductRequest command = new SaveProductRequest(
                "Producto", "Desc", BigDecimal.TEN, BigDecimal.ONE, false,
                null, "Cat Existente", null, null, null, null
        );

        CategoriaProducto existingCat = new CategoriaProducto();
        existingCat.setDescripcion("Cat Existente");
        when(categoryRepository.findByDescription("Cat Existente")).thenReturn(java.util.Optional.of(existingCat));
        when(productRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        ProductResponse result = saveProduct.execute(command);

        assertEquals(existingCat.getId(), result.categoria().id());
        assertEquals(existingCat.getDescripcion(), result.categoria().descripcion());
    }

    @Test
    void execute_ActualizaInventarioExistente_CuandoYaExiste() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        SaveProductRequest command = new SaveProductRequest(
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
        SaveProductRequest command = new SaveProductRequest(
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
                20,
                null
        );
        Producto savedProduct = new Producto();
        savedProduct.setId(productId);
        when(productRepository.save(any(Producto.class))).thenReturn(savedProduct);
        when(inventoryRepository.findByBuffetAndProduct(buffetId, productId))
                .thenReturn(java.util.Optional.empty());

        ProductResponse result = saveProduct.execute(command);

        assertNull(result.stockActual());
        assertNull(result.stockDisponible());
        assertEquals(TipoManejoInventario.CUPO_DIARIO.name(), result.tipoManejoInventario());
        assertEquals(20, result.cupoMaximoDiario());
        verify(inventoryRepository).save(any(Inventario.class));
    }

    @Test
    void execute_MapeaClasificacionesYTipos_CuandoSeProveen() {
        UUID saludId = UUID.randomUUID();
        UUID tipoId = UUID.randomUUID();
        SaveProductRequest command = new SaveProductRequest(
                "Prod", null, null, null, true,
                null, null, null, null, Set.of(saludId), Set.of(tipoId)
        );

        when(productRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        ProductResponse result = saveProduct.execute(command);

        assertEquals(1, result.clasificacionesSalud().size());
        assertEquals(saludId, result.clasificacionesSalud().iterator().next().id());
        assertEquals(1, result.tipos().size());
        assertEquals(tipoId, result.tipos().iterator().next().id());
    }

    @Test
    void execute_NoAsignaCategoria_CuandoAmbosCamposSonNulos() {
        SaveProductRequest command = new SaveProductRequest(
                "Prod", null, null, null, true,
                null, "", null, null, null, null
        );

        when(productRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        ProductResponse result = saveProduct.execute(command);

        assertNull(result.categoria());
    }

    @Test
    void execute_NoGuardaInventario_CuandoStockEsNulo() {
        UUID buffetId = UUID.randomUUID();
        SaveProductRequest command = new SaveProductRequest(
                "Prod", null, null, null, true,
                null, null, buffetId, null, null, null
        );

        when(productRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        saveProduct.execute(command);

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void execute_NoGuardaInventario_CuandoBuffetIdEsNulo() {
        SaveProductRequest command = new SaveProductRequest(
                "Prod", null, null, null, true,
                null, null, null, 100, null, null
        );

        when(productRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        saveProduct.execute(command);

        verify(inventoryRepository, never()).save(any());
    }
}
