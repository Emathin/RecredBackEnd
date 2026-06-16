package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.models.DailyInventorySnapshot;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.infrastructure.entities.CierreDiarioBuffetEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.recred.backend.infrastructure.persistence.DailyBuffetCloseRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataDailyBuffetCloseRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
