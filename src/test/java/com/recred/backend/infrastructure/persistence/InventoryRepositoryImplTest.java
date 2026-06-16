package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import com.recred.backend.infrastructure.entities.InventarioBuffetEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryRepositoryImplTest {

    @Mock
    private SpringDataInventoryRepository springDataInventoryRepository;

    private InventoryRepositoryImpl inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository = new InventoryRepositoryImpl(springDataInventoryRepository);
    }

    @Test
    void save_DeberiaCrearNuevaEntidad_CuandoNoExiste() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Inventario inventario = new Inventario();
        inventario.setBuffetId(buffetId);
        inventario.setProductoId(productId);
        inventario.setStockActual(10);
        inventario.setEstado(EstadoInventario.DISPONIBLE);

        when(springDataInventoryRepository.findByBuffetEntityIdAndProductoEntityId(buffetId, productId))
                .thenReturn(Optional.empty());
        when(springDataInventoryRepository.save(any(InventarioBuffetEntity.class))).thenAnswer(i -> i.getArgument(0));

        inventoryRepository.save(inventario);

        verify(springDataInventoryRepository).save(any(InventarioBuffetEntity.class));
    }

    @Test
    void save_DeberiaActualizarEntidad_CuandoYaExiste() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Inventario inventario = new Inventario();
        inventario.setBuffetId(buffetId);
        inventario.setProductoId(productId);
        inventario.setStockActual(50);

        InventarioBuffetEntity existingEntity = new InventarioBuffetEntity();
        existingEntity.setStockActual(10);
        existingEntity.setBuffetEntity(new BuffetEntity());
        existingEntity.setProductoEntity(new ProductoEntity());

        when(springDataInventoryRepository.findByBuffetEntityIdAndProductoEntityId(buffetId, productId))
                .thenReturn(Optional.of(existingEntity));
        when(springDataInventoryRepository.save(any(InventarioBuffetEntity.class))).thenAnswer(i -> i.getArgument(0));

        inventoryRepository.save(inventario);

        assertEquals(50, existingEntity.getStockActual());
    }

    @Test
    void save_CuandoEstadoGuardadoNoCoincideConStock_DeberiaPersistirEstadoEfectivo() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Inventario inventario = new Inventario();
        inventario.setBuffetId(buffetId);
        inventario.setProductoId(productId);
        inventario.setStockActual(4);
        inventario.setStockReservado(0);
        inventario.setStockMinimo(null);
        inventario.setEstado(EstadoInventario.DISPONIBLE);

        when(springDataInventoryRepository.findByBuffetEntityIdAndProductoEntityId(buffetId, productId))
                .thenReturn(Optional.empty());
        when(springDataInventoryRepository.save(any(InventarioBuffetEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        inventoryRepository.save(inventario);

        ArgumentCaptor<InventarioBuffetEntity> captor =
                ArgumentCaptor.forClass(InventarioBuffetEntity.class);
        verify(springDataInventoryRepository).save(captor.capture());
        assertEquals(EstadoInventario.BAJO_STOCK, captor.getValue().getEstado());
        assertEquals(Inventario.STOCK_MINIMO_PREDETERMINADO,
                captor.getValue().getStockMinimo());
    }

    @Test
    void findByBuffetAndProduct_DeberiaLlamarAlSpringData() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        inventoryRepository.findByBuffetAndProduct(buffetId, productId);

        verify(springDataInventoryRepository).findByBuffetEntityIdAndProductoEntityId(buffetId, productId);
    }

    @Test
    void findLockedByBuffetAndProduct_ShouldReturnMappedInventory() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        InventarioBuffetEntity entity = inventoryEntity(buffetId, productId);

        when(springDataInventoryRepository.findLockedByBuffetEntityIdAndProductoEntityId(
                buffetId,
                productId
        )).thenReturn(Optional.of(entity));

        Optional<Inventario> result = inventoryRepository.findLockedByBuffetAndProduct(
                buffetId,
                productId
        );

        assertTrue(result.isPresent());
        assertEquals(buffetId, result.orElseThrow().getBuffetId());
        assertEquals(productId, result.orElseThrow().getProductoId());
    }

    @Test
    void save_WhenInventoryReturnsNullStateAndType_ShouldUseDefaults() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Inventario inventario = mock(Inventario.class);

        when(inventario.getBuffetId()).thenReturn(buffetId);
        when(inventario.getProductoId()).thenReturn(productId);
        when(inventario.getStockActual()).thenReturn(null);
        when(inventario.getStockReservado()).thenReturn(null);
        when(inventario.stockMinimoEfectivo())
                .thenReturn(Inventario.STOCK_MINIMO_PREDETERMINADO);
        when(inventario.estadoActual()).thenReturn(null);
        when(inventario.getTipoManejo()).thenReturn(null);
        when(inventario.getCupoMaximoDiario()).thenReturn(null);
        when(springDataInventoryRepository.findByBuffetEntityIdAndProductoEntityId(
                buffetId,
                productId
        )).thenReturn(Optional.empty());
        when(springDataInventoryRepository.save(any(InventarioBuffetEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        inventoryRepository.save(inventario);

        ArgumentCaptor<InventarioBuffetEntity> captor =
                ArgumentCaptor.forClass(InventarioBuffetEntity.class);
        verify(springDataInventoryRepository).save(captor.capture());
        assertEquals(EstadoInventario.DISPONIBLE, captor.getValue().getEstado());
        assertEquals(TipoManejoInventario.STOCK_EXACTO, captor.getValue().getTipoManejo());
        assertEquals(0, captor.getValue().getStockActual());
        assertEquals(0, captor.getValue().getStockReservado());
    }

    @Test
    void checkIfProductIsInStock_DeberiaLlamarAlSpringDataYRetornarElResultado() {
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(springDataInventoryRepository.existsByProductIdAndKiosqueroId(productId, buffetId))
                .thenReturn(true);

        boolean result = inventoryRepository.checkIfProductIsInStock(productId, buffetId);

        assertTrue(result);
        verify(springDataInventoryRepository).existsByProductIdAndKiosqueroId(productId, buffetId);
    }

    @Test
    void findAvailableProductsByBuffet_ShouldMapAvailableProductsToDomain() {
        UUID buffetId = UUID.randomUUID();

        ProductoEntity productoEntity = new ProductoEntity();
        productoEntity.setId(UUID.randomUUID());
        productoEntity.setNombre("Jugo");

        CategoriaProductoEntity categoria = new CategoriaProductoEntity();
        categoria.setDescripcion("Bebidas");
        productoEntity.setCategoria(categoria);

        InventarioBuffetEntity inventario = new InventarioBuffetEntity();
        inventario.setProductoEntity(productoEntity);

        when(springDataInventoryRepository.findAvailableByBuffetId(buffetId))
                .thenReturn(List.of(inventario));

        List<Producto> result = inventoryRepository.findAvailableProductsByBuffet(buffetId);

        assertEquals(1, result.size());
        assertEquals("Jugo", result.get(0).getNombre());
        assertEquals("Bebidas", result.get(0).getCategoria().getDescripcion());
    }

    @Test
    void findAvailableProductsByBuffet_ShouldHandleNullCategory() {
        UUID buffetId = UUID.randomUUID();

        ProductoEntity productoEntity = new ProductoEntity();
        productoEntity.setId(UUID.randomUUID());
        productoEntity.setNombre("Fruta");
        productoEntity.setCategoria(null);

        InventarioBuffetEntity inventario = new InventarioBuffetEntity();
        inventario.setProductoEntity(productoEntity);

        when(springDataInventoryRepository.findAvailableByBuffetId(buffetId))
                .thenReturn(List.of(inventario));

        List<Producto> result = inventoryRepository.findAvailableProductsByBuffet(buffetId);

        assertNull(result.get(0).getCategoria());
    }

    private InventarioBuffetEntity inventoryEntity(UUID buffetId, UUID productId) {
        BuffetEntity buffetEntity = new BuffetEntity();
        buffetEntity.setId(buffetId);
        ProductoEntity productoEntity = new ProductoEntity();
        productoEntity.setId(productId);
        InventarioBuffetEntity entity = new InventarioBuffetEntity();
        entity.setBuffetEntity(buffetEntity);
        entity.setProductoEntity(productoEntity);
        entity.setStockActual(10);
        entity.setStockReservado(2);
        entity.setStockMinimo(4);
        entity.setEstado(EstadoInventario.DISPONIBLE);
        entity.setTipoManejo(TipoManejoInventario.STOCK_EXACTO);
        return entity;
    }
}
