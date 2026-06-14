package com.recred.backend.domain.services;

import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimeEventPublisher;
import com.recred.backend.domain.events.KiosqueroRealtimeEventType;
import com.recred.backend.domain.exceptions.InventoryNotFoundException;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.InventoryQuickActionType;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.MovimientoStock;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.models.TipoMovimientoStock;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryAvailabilityServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private StockMovementRepository stockMovementRepository;
    @Mock
    private KiosqueroRealtimeEventPublisher realtimeEventPublisher;

    private InventoryAvailabilityService service;
    private UUID buffetId;
    private UUID productId;
    private Inventario inventory;
    private Producto product;

    @BeforeEach
    void setUp() {
        service = new InventoryAvailabilityService(
                inventoryRepository,
                stockMovementRepository,
                realtimeEventPublisher
        );
        buffetId = UUID.randomUUID();
        productId = UUID.randomUUID();
        inventory = inventory(10, 2);
        product = new Producto();
        product.setId(productId);
    }

    @Test
    void reserve_ShouldReserveAndRegisterMovement() {
        when(inventoryRepository.findLockedByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);

        service.reserve(buffetId, Map.of(product, 3));

        assertEquals(5, inventory.getStockReservado());
        assertMovement(TipoMovimientoStock.RESERVA, 3);
        assertRealtimeEvent(KiosqueroRealtimeEventType.STOCK_CHANGED);
    }

    @Test
    void consumeDirectly_ShouldConsumeAndRegisterMovement() {
        when(inventoryRepository.findLockedByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);

        service.consumeDirectly(buffetId, Map.of(product, 3));

        assertEquals(7, inventory.getStockActual());
        assertMovement(TipoMovimientoStock.VENTA, 3);
    }

    @Test
    void releaseReservation_ShouldReleaseAndRegisterMovement() {
        when(inventoryRepository.findLockedByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);

        service.releaseReservation(buffetId, Map.of(product, 2));

        assertEquals(0, inventory.getStockReservado());
        assertMovement(TipoMovimientoStock.LIBERACION, 2);
    }

    @Test
    void consumeReservation_ShouldConsumeReservedStockAndRegisterMovement() {
        when(inventoryRepository.findLockedByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);

        service.consumeReservation(buffetId, Map.of(product, 2));

        assertEquals(8, inventory.getStockActual());
        assertEquals(0, inventory.getStockReservado());
        assertMovement(TipoMovimientoStock.CONSUMO, 2);
    }

    @Test
    void adjustStock_ShouldUpdateStockAndRegisterMovement() {
        UUID userId = UUID.randomUUID();
        when(inventoryRepository.findLockedByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);

        service.adjustStock(buffetId, productId, 14, "Ajuste manual", userId);

        assertEquals(14, inventory.getStockActual());
        ArgumentCaptor<MovimientoStock> captor =
                ArgumentCaptor.forClass(MovimientoStock.class);
        verify(stockMovementRepository).save(captor.capture());
        assertEquals(inventory.getId(), captor.getValue().inventarioId());
        assertEquals(TipoMovimientoStock.AJUSTE, captor.getValue().tipo());
        assertEquals(4, captor.getValue().cantidad());
        assertEquals("Ajuste manual", captor.getValue().motivo());
        assertEquals(userId, captor.getValue().usuarioId());
        assertRealtimeEvent(KiosqueroRealtimeEventType.STOCK_CHANGED);
    }

    @Test
    void consumeDirectly_WhenProductIsSoldOut_ShouldPublishSoldOutEvent() {
        inventory = inventory(3, 0);
        when(inventoryRepository.findLockedByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);

        service.consumeDirectly(buffetId, Map.of(product, 3));

        assertEquals(EstadoInventario.SIN_STOCK, inventory.getEstado());
        assertRealtimeEvent(KiosqueroRealtimeEventType.PRODUCT_SOLD_OUT);
    }

    @Test
    void consumeDirectly_WhenProductReachesLowStock_ShouldPublishLowStockEvent() {
        inventory = inventory(6, 0);
        inventory.setStockMinimo(5);
        when(inventoryRepository.findLockedByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);

        service.consumeDirectly(buffetId, Map.of(product, 1));

        assertEquals(EstadoInventario.BAJO_STOCK, inventory.getEstado());
        assertRealtimeEvent(KiosqueroRealtimeEventType.LOW_STOCK);
    }

    @Test
    void applyQuickAction_WhenInventoryDoesNotExist_ShouldCreateSimpleStock() {
        UUID userId = UUID.randomUUID();
        when(inventoryRepository.findLockedByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventario.class))).thenAnswer(invocation -> {
            Inventario saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        Inventario result = service.applyQuickAction(
                buffetId,
                productId,
                InventoryQuickActionType.SET_STOCK,
                12,
                3,
                null,
                userId
        );

        assertEquals(TipoManejoInventario.STOCK_EXACTO, result.getTipoManejo());
        assertEquals(12, result.getStockActual());
        assertEquals(3, result.getStockMinimo());
        ArgumentCaptor<MovimientoStock> captor =
                ArgumentCaptor.forClass(MovimientoStock.class);
        verify(stockMovementRepository).save(captor.capture());
        assertEquals(result.getId(), captor.getValue().inventarioId());
        assertEquals(TipoMovimientoStock.AJUSTE, captor.getValue().tipo());
        assertEquals(12, captor.getValue().cantidad());
    }

    @Test
    void applyQuickAction_WhenInventoryDoesNotExistAndNoMinimumProvided_ShouldUseDefaultMinimum() {
        when(inventoryRepository.findLockedByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventario.class))).thenAnswer(invocation -> {
            Inventario saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        Inventario result = service.applyQuickAction(
                buffetId,
                productId,
                InventoryQuickActionType.SET_STOCK,
                4,
                null,
                null,
                null
        );

        assertEquals(Inventario.STOCK_MINIMO_PREDETERMINADO, result.getStockMinimo());
        assertEquals(EstadoInventario.BAJO_STOCK, result.getEstado());
    }

    @Test
    void applyQuickAction_SetUnavailable_ShouldUseSimpleAvailabilityMode() {
        when(inventoryRepository.findLockedByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);

        service.applyQuickAction(
                buffetId,
                productId,
                InventoryQuickActionType.SET_UNAVAILABLE,
                null,
                null,
                null,
                null
        );

        assertEquals(TipoManejoInventario.DISPONIBLE_NO_DISPONIBLE,
                inventory.getTipoManejo());
        assertEquals(EstadoInventario.SIN_STOCK, inventory.getEstado());
        assertMovement(TipoMovimientoStock.AJUSTE, 8);
    }

    @Test
    void applyQuickAction_MarkSoldOut_WhenInventoryDoesNotExist_ShouldFailWithoutCreatingInventory() {
        when(inventoryRepository.findLockedByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.empty());

        assertThrows(InventoryNotFoundException.class, () -> service.applyQuickAction(
                buffetId,
                productId,
                InventoryQuickActionType.MARK_SOLD_OUT,
                null,
                null,
                "Producto agotado",
                null
        ));

        verify(inventoryRepository, never()).save(any(Inventario.class));
        verify(stockMovementRepository, never()).save(any(MovimientoStock.class));
    }

    @Test
    void applyQuickAction_SetUnavailable_WhenInventoryDoesNotExist_ShouldFailWithoutCreatingInventory() {
        when(inventoryRepository.findLockedByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.empty());

        assertThrows(InventoryNotFoundException.class, () -> service.applyQuickAction(
                buffetId,
                productId,
                InventoryQuickActionType.SET_UNAVAILABLE,
                null,
                null,
                null,
                null
        ));

        verify(inventoryRepository, never()).save(any(Inventario.class));
        verify(stockMovementRepository, never()).save(any(MovimientoStock.class));
    }

    @Test
    void applyQuickAction_MarkSoldOut_WhenAlreadySoldOut_ShouldNotRegisterZeroMovement() {
        inventory = inventory(0, 0);
        inventory.setEstado(EstadoInventario.SIN_STOCK);
        when(inventoryRepository.findLockedByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);

        Inventario result = service.applyQuickAction(
                buffetId,
                productId,
                InventoryQuickActionType.MARK_SOLD_OUT,
                null,
                null,
                "Producto agotado",
                null
        );

        assertEquals(EstadoInventario.SIN_STOCK, result.getEstado());
        verify(inventoryRepository).save(inventory);
        verify(stockMovementRepository, never()).save(any(MovimientoStock.class));
    }

    @Test
    void applyQuickAction_SetDailyCapacity_ShouldUseDailyCapacityMode() {
        when(inventoryRepository.findLockedByBuffetAndProduct(buffetId, productId))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);

        service.applyQuickAction(
                buffetId,
                productId,
                InventoryQuickActionType.SET_DAILY_CAPACITY,
                20,
                null,
                null,
                null
        );

        assertEquals(TipoManejoInventario.CUPO_DIARIO, inventory.getTipoManejo());
        assertEquals(20, inventory.getStockActual());
        assertEquals(20, inventory.getCupoMaximoDiario());
        assertMovement(TipoMovimientoStock.AJUSTE, 10);
    }

    private void assertMovement(TipoMovimientoStock type, int quantity) {
        ArgumentCaptor<MovimientoStock> captor =
                ArgumentCaptor.forClass(MovimientoStock.class);
        verify(stockMovementRepository).save(captor.capture());
        assertEquals(inventory.getId(), captor.getValue().inventarioId());
        assertEquals(type, captor.getValue().tipo());
        assertEquals(quantity, captor.getValue().cantidad());
    }

    private void assertRealtimeEvent(KiosqueroRealtimeEventType type) {
        ArgumentCaptor<KiosqueroRealtimeEvent> captor =
                ArgumentCaptor.forClass(KiosqueroRealtimeEvent.class);
        verify(realtimeEventPublisher, atLeastOnce()).publish(captor.capture());
        boolean found = captor.getAllValues().stream()
                .anyMatch(event -> event.type() == type
                        && event.buffetId().equals(buffetId)
                        && event.productId().equals(productId));
        assertTrue(found);
    }

    private Inventario inventory(int stockActual, int stockReservado) {
        Inventario inventario = new Inventario();
        inventario.setId(UUID.randomUUID());
        inventario.setBuffetId(buffetId);
        inventario.setProductoId(productId);
        inventario.setStockActual(stockActual);
        inventario.setStockReservado(stockReservado);
        inventario.setStockMinimo(1);
        inventario.setEstado(EstadoInventario.DISPONIBLE);
        inventario.setTipoManejo(TipoManejoInventario.STOCK_EXACTO);
        return inventario;
    }
}
