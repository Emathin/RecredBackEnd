package com.recred.backend.integration.persistence;

import com.recred.backend.infrastructure.persistence.*;

import com.recred.backend.domain.models.MovimientoStock;
import com.recred.backend.domain.models.TipoMovimientoStock;
import com.recred.backend.infrastructure.entities.InventarioBuffetEntity;
import com.recred.backend.infrastructure.entities.MovimientoStockEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockMovementRepositoryImplTest {

    @Mock
    private SpringDataStockMovementRepository springDataRepository;

    private StockMovementRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new StockMovementRepositoryImpl(springDataRepository);
    }

    @Test
    void saveShouldMapMovementAndUserReference() {
        UUID movementId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID purchaseId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        MovimientoStock movement = new MovimientoStock(
                movementId,
                inventoryId,
                TipoMovimientoStock.INGRESO,
                5,
                1,
                6,
                "Carga",
                userId,
                purchaseId,
                createdAt
        );
        when(springDataRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoStock result = repository.save(movement);

        ArgumentCaptor<MovimientoStockEntity> captor = ArgumentCaptor.forClass(
                MovimientoStockEntity.class
        );
        verify(springDataRepository).save(captor.capture());
        MovimientoStockEntity saved = captor.getValue();
        assertEquals(movementId, saved.getId());
        assertEquals(inventoryId, saved.getInventario().getId());
        assertEquals(TipoMovimientoStock.INGRESO, saved.getTipo());
        assertEquals(5, saved.getCantidad());
        assertEquals(1, saved.getCantidadAnterior());
        assertEquals(6, saved.getCantidadNueva());
        assertEquals("Carga", saved.getMotivo());
        assertEquals(userId, saved.getCreadoPorUsuarioEntity().getId());
        assertEquals(purchaseId, saved.getCompraId());
        assertEquals(movementId, result.id());
        assertEquals(inventoryId, result.inventarioId());
        assertEquals(userId, result.usuarioId());
    }

    @Test
    void saveShouldHandleNullUserId() {
        MovimientoStock movement = new MovimientoStock(
                UUID.randomUUID(),
                UUID.randomUUID(),
                TipoMovimientoStock.AJUSTE,
                2,
                4,
                6,
                "Ajuste",
                null,
                null,
                null
        );
        when(springDataRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoStock result = repository.save(movement);

        ArgumentCaptor<MovimientoStockEntity> captor = ArgumentCaptor.forClass(
                MovimientoStockEntity.class
        );
        verify(springDataRepository).save(captor.capture());
        assertNull(captor.getValue().getCreadoPorUsuarioEntity());
        assertNull(result.usuarioId());
    }

    @Test
    void findByInventoryIdShouldMapEntitiesToDomain() {
        UUID inventoryId = UUID.randomUUID();
        MovimientoStockEntity entity = entity(inventoryId, UUID.randomUUID());
        when(springDataRepository.findAllByInventarioIdOrderByCreadoEnDesc(inventoryId))
                .thenReturn(List.of(entity));

        List<MovimientoStock> result = repository.findByInventoryId(inventoryId);

        assertEquals(1, result.size());
        assertEquals(entity.getId(), result.get(0).id());
        assertEquals(inventoryId, result.get(0).inventarioId());
        assertEquals(entity.getCreadoPorUsuarioEntity().getId(), result.get(0).usuarioId());
    }

    @Test
    void findByBuffetAndDateRangeShouldMapEntitiesToDomain() {
        UUID buffetId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 15, 23, 59);
        MovimientoStockEntity entity = entity(inventoryId, null);
        when(springDataRepository.findByBuffetAndDateRange(buffetId, start, end))
                .thenReturn(List.of(entity));

        List<MovimientoStock> result = repository.findByBuffetAndDateRange(
                buffetId,
                start,
                end
        );

        assertEquals(1, result.size());
        assertEquals(entity.getId(), result.get(0).id());
        assertNull(result.get(0).usuarioId());
    }

    private MovimientoStockEntity entity(UUID inventoryId, UUID userId) {
        InventarioBuffetEntity inventory = new InventarioBuffetEntity();
        inventory.setId(inventoryId);
        MovimientoStockEntity entity = new MovimientoStockEntity();
        entity.setId(UUID.randomUUID());
        entity.setInventario(inventory);
        entity.setTipo(TipoMovimientoStock.VENTA);
        entity.setCantidad(2);
        entity.setCantidadAnterior(4);
        entity.setCantidadNueva(2);
        entity.setMotivo("Venta");
        entity.setCreadoEn(LocalDateTime.of(2026, 6, 15, 10, 0));
        if (userId != null) {
            UsuarioEntity user = new UsuarioEntity();
            user.setId(userId);
            entity.setCreadoPorUsuarioEntity(user);
        }
        return entity;
    }
}
