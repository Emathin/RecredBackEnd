package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.models.DailyInventorySnapshot;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.CierreDiarioBuffetEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyBuffetCloseRepositoryImplTest {

    @Test
    void saveWithInventorySnapshot_ShouldPersistSnapshotWithClose() {
        SpringDataDailyBuffetCloseRepository springDataRepository =
                mock(SpringDataDailyBuffetCloseRepository.class);
        DailyBuffetCloseRepositoryImpl repository =
                new DailyBuffetCloseRepositoryImpl(springDataRepository);
        DailyBuffetClose close = new DailyBuffetClose(
                null,
                UUID.randomUUID(),
                LocalDate.of(2026, 6, 12),
                1,
                2,
                BigDecimal.ZERO
        );
        DailyInventorySnapshot snapshot = snapshot();
        when(springDataRepository.save(any(CierreDiarioBuffetEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        repository.saveWithInventorySnapshot(close, List.of(snapshot));

        ArgumentCaptor<CierreDiarioBuffetEntity> entityCaptor =
                ArgumentCaptor.forClass(CierreDiarioBuffetEntity.class);
        verify(springDataRepository).save(entityCaptor.capture());
        CierreDiarioBuffetEntity entity = entityCaptor.getValue();
        assertEquals(close.buffetId(), entity.getBuffetEntity().getId());
        assertEquals(close.date(), entity.getFecha());
        assertEquals(List.of(snapshot), entity.inventarioSnapshotToDomain());
    }

    @Test
    void findByBuffetAndDateShouldMapEntityToDomain() {
        SpringDataDailyBuffetCloseRepository springDataRepository =
                mock(SpringDataDailyBuffetCloseRepository.class);
        DailyBuffetCloseRepositoryImpl repository =
                new DailyBuffetCloseRepositoryImpl(springDataRepository);
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 12);
        CierreDiarioBuffetEntity entity = closeEntity(buffetId, date);
        when(springDataRepository.findByBuffetEntityIdAndFecha(buffetId, date))
                .thenReturn(Optional.of(entity));

        Optional<DailyBuffetClose> result = repository.findByBuffetAndDate(buffetId, date);

        assertTrue(result.isPresent());
        assertEquals(buffetId, result.orElseThrow().buffetId());
        assertEquals(date, result.orElseThrow().date());
    }

    @Test
    void findByBuffetAndDateRangeShouldMapEntitiesToDomain() {
        SpringDataDailyBuffetCloseRepository springDataRepository =
                mock(SpringDataDailyBuffetCloseRepository.class);
        DailyBuffetCloseRepositoryImpl repository =
                new DailyBuffetCloseRepositoryImpl(springDataRepository);
        UUID buffetId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 6, 1);
        LocalDate endDate = LocalDate.of(2026, 6, 30);
        CierreDiarioBuffetEntity entity = closeEntity(buffetId, LocalDate.of(2026, 6, 12));
        when(springDataRepository.findByBuffetAndDateRange(buffetId, startDate, endDate))
                .thenReturn(List.of(entity));

        List<DailyBuffetClose> result = repository.findByBuffetAndDateRange(
                buffetId,
                startDate,
                endDate
        );

        assertEquals(1, result.size());
        assertEquals(buffetId, result.get(0).buffetId());
    }

    @Test
    void saveShouldPersistCloseWithoutInventorySnapshot() {
        SpringDataDailyBuffetCloseRepository springDataRepository =
                mock(SpringDataDailyBuffetCloseRepository.class);
        DailyBuffetCloseRepositoryImpl repository =
                new DailyBuffetCloseRepositoryImpl(springDataRepository);
        DailyBuffetClose close = new DailyBuffetClose(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.of(2026, 6, 12),
                3,
                4,
                new BigDecimal("1500.00")
        );
        when(springDataRepository.save(any(CierreDiarioBuffetEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DailyBuffetClose result = repository.save(close);

        assertEquals(close.buffetId(), result.buffetId());
        assertEquals(close.expiredPurchases(), result.expiredPurchases());
        ArgumentCaptor<CierreDiarioBuffetEntity> entityCaptor =
                ArgumentCaptor.forClass(CierreDiarioBuffetEntity.class);
        verify(springDataRepository).save(entityCaptor.capture());
        assertEquals(close.refundedCredits(), entityCaptor.getValue().getCreditosDevueltos());
    }

    @Test
    void findInventorySnapshotByBuffetAndDateShouldReturnSnapshotWhenPresent() {
        SpringDataDailyBuffetCloseRepository springDataRepository =
                mock(SpringDataDailyBuffetCloseRepository.class);
        DailyBuffetCloseRepositoryImpl repository =
                new DailyBuffetCloseRepositoryImpl(springDataRepository);
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 12);
        CierreDiarioBuffetEntity entity = closeEntity(buffetId, date);
        entity.setInventarioSnapshotFromDomain(List.of(snapshot()));
        when(springDataRepository.findWithInventorySnapshotByBuffetAndDate(buffetId, date))
                .thenReturn(Optional.of(entity));

        Optional<List<DailyInventorySnapshot>> result =
                repository.findInventorySnapshotByBuffetAndDate(buffetId, date);

        assertTrue(result.isPresent());
        assertEquals(1, result.orElseThrow().size());
    }

    @Test
    void findInventorySnapshotByBuffetAndDateShouldReturnEmptyWhenSnapshotIsEmpty() {
        SpringDataDailyBuffetCloseRepository springDataRepository =
                mock(SpringDataDailyBuffetCloseRepository.class);
        DailyBuffetCloseRepositoryImpl repository =
                new DailyBuffetCloseRepositoryImpl(springDataRepository);
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 12);
        CierreDiarioBuffetEntity entity = closeEntity(buffetId, date);
        when(springDataRepository.findWithInventorySnapshotByBuffetAndDate(buffetId, date))
                .thenReturn(Optional.of(entity));

        Optional<List<DailyInventorySnapshot>> result =
                repository.findInventorySnapshotByBuffetAndDate(buffetId, date);

        assertTrue(result.isEmpty());
    }

    private DailyInventorySnapshot snapshot() {
        return new DailyInventorySnapshot(
                UUID.randomUUID(),
                "Alfajor",
                10,
                2,
                8,
                3,
                null,
                null,
                EstadoInventario.DISPONIBLE,
                TipoManejoInventario.STOCK_EXACTO
        );
    }

    private CierreDiarioBuffetEntity closeEntity(UUID buffetId, LocalDate date) {
        BuffetEntity buffet = new BuffetEntity();
        buffet.setId(buffetId);
        CierreDiarioBuffetEntity entity = new CierreDiarioBuffetEntity();
        entity.setId(UUID.randomUUID());
        entity.setBuffetEntity(buffet);
        entity.setFecha(date);
        entity.setPedidosVencidos(2);
        entity.setReservasLiberadas(3);
        entity.setCreditosDevueltos(BigDecimal.TEN);
        return entity;
    }
}
