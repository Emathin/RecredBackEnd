package com.recred.backend.unit.application;

import com.recred.backend.application.UpdateProduct;
import com.recred.backend.application.UpdateProductCommand;
import com.recred.backend.domain.exceptions.ProductNotFoundException;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
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
import java.util.Set;
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

    @Test
    void dadoQueVieneCategoriaNuevaExistente_cuandoExecute_entoncesAsignaCategoria() {
        UUID id = UUID.randomUUID();
        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());
        category.setDescripcion("Frutas");
        UpdateProductCommand command = new UpdateProductCommand(
                id, "Nombre Act", "Desc Act", BigDecimal.TEN, new BigDecimal("0.5"), false,
                UUID.randomUUID(), "Frutas", null, null, null, null
        );
        Producto existing = new Producto();
        existing.setId(id);

        when(productRepository.findActiveById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByDescription("Frutas")).thenReturn(Optional.of(category));
        when(productRepository.save(existing)).thenReturn(existing);

        updateProduct.execute(command);

        assertEquals(category, existing.getCategoria());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void dadoQueVieneCategoriaNuevaInexistente_cuandoExecute_entoncesCreaCategoria() {
        UUID id = UUID.randomUUID();
        UpdateProductCommand command = new UpdateProductCommand(
                id, "Nombre Act", "Desc Act", BigDecimal.TEN, new BigDecimal("0.5"), false,
                null, "Nueva Categoria", null, null, null, null
        );
        Producto existing = new Producto();
        existing.setId(id);
        CategoriaProducto savedCategory = new CategoriaProducto();
        savedCategory.setId(UUID.randomUUID());
        savedCategory.setDescripcion("Nueva Categoria");

        when(productRepository.findActiveById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByDescription("Nueva Categoria")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(CategoriaProducto.class))).thenReturn(savedCategory);
        when(productRepository.save(existing)).thenReturn(existing);

        updateProduct.execute(command);

        assertEquals(savedCategory, existing.getCategoria());
        verify(categoryRepository).save(any(CategoriaProducto.class));
    }

    @Test
    void dadoQueVieneCategoriaIdSinNombreNuevo_cuandoExecute_entoncesAsignaProxy() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UpdateProductCommand command = new UpdateProductCommand(
                id, "Nombre Act", "Desc Act", BigDecimal.TEN, new BigDecimal("0.5"), false,
                categoryId, " ", null, null, null, null
        );
        Producto existing = new Producto();
        existing.setId(id);

        when(productRepository.findActiveById(id)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        updateProduct.execute(command);

        assertEquals(categoryId, existing.getCategoria().getId());
        verify(categoryRepository, never()).findByDescription(any());
    }

    @Test
    void dadoQueVienenClasificacionesYTipos_cuandoExecute_entoncesLosActualiza() {
        UUID id = UUID.randomUUID();
        UUID healthId = UUID.randomUUID();
        UUID typeId = UUID.randomUUID();
        UpdateProductCommand command = new UpdateProductCommand(
                id, "Nombre Act", "Desc Act", BigDecimal.TEN, new BigDecimal("0.5"), false,
                null, null, null, null, Set.of(healthId), Set.of(typeId)
        );
        Producto existing = new Producto();
        existing.setId(id);

        when(productRepository.findActiveById(id)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        updateProduct.execute(command);

        assertEquals(healthId, existing.getClasificacionesSalud().iterator().next().getId());
        assertEquals(typeId, existing.getTipos().iterator().next().getId());
    }

    @Test
    void dadoQueNoExisteInventario_cuandoExecute_entoncesCreaUnoNuevo() {
        UUID id = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UpdateProductCommand command = new UpdateProductCommand(
                id,
                "Nombre Act",
                "Desc Act",
                BigDecimal.TEN,
                new BigDecimal("0.5"),
                false,
                null,
                null,
                buffetId,
                null,
                null,
                null,
                TipoManejoInventario.CUPO_DIARIO,
                25,
                null
        );
        Producto existing = new Producto();
        existing.setId(id);

        when(productRepository.findActiveById(id)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);
        when(inventoryRepository.findByBuffetAndProduct(buffetId, id))
                .thenReturn(Optional.empty());

        updateProduct.execute(command);

        verify(inventoryRepository).save(argThat(inventory ->
                buffetId.equals(inventory.getBuffetId())
                        && id.equals(inventory.getProductoId())
                        && TipoManejoInventario.CUPO_DIARIO.equals(inventory.getTipoManejo())
                        && Integer.valueOf(25).equals(inventory.getCupoMaximoDiario())
        ));
    }

    @Test
    void dadoQueVieneSoloCupoDiario_cuandoNoExisteInventario_entoncesUsaTipoPorDefecto() {
        UUID id = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UpdateProductCommand command = new UpdateProductCommand(
                id,
                "Nombre Act",
                "Desc Act",
                BigDecimal.TEN,
                new BigDecimal("0.5"),
                false,
                null,
                null,
                buffetId,
                null,
                null,
                null,
                null,
                8,
                null
        );
        Producto existing = new Producto();
        existing.setId(id);

        when(productRepository.findActiveById(id)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);
        when(inventoryRepository.findByBuffetAndProduct(buffetId, id))
                .thenReturn(Optional.empty());

        updateProduct.execute(command);

        verify(inventoryRepository).save(argThat(inventory ->
                TipoManejoInventario.STOCK_EXACTO.equals(inventory.getTipoManejo())
                        && Integer.valueOf(8).equals(inventory.getCupoMaximoDiario())
                        && inventory.getStockActual() == null
        ));
    }
}
