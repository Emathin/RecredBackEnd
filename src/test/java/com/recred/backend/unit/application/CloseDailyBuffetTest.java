package com.recred.backend.unit.application;

import com.recred.backend.application.CloseDailyBuffet;
import com.recred.backend.application.ExpireAdvancePurchases;
import com.recred.backend.application.ExpireAdvancePurchases.ExpirationResult;
import com.recred.backend.application.GenerateDailyBuffetReport;
import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimeEventPublisher;
import com.recred.backend.domain.events.KiosqueroRealtimeEventType;
import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.models.DailyBuffetReport;
import com.recred.backend.domain.models.DailyCloseResult;
import com.recred.backend.domain.models.DailyInventorySnapshot;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloseDailyBuffetTest {

    @Test
    void execute_WhenCloseDoesNotExist_ShouldExpireAndSaveClose() {
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 8);
        ExpireAdvancePurchases expireAdvancePurchases = mock(ExpireAdvancePurchases.class);
        GenerateDailyBuffetReport generateDailyBuffetReport =
                mock(GenerateDailyBuffetReport.class);
        DailyBuffetCloseRepository closeRepository = mock(DailyBuffetCloseRepository.class);
        KiosqueroRealtimeEventPublisher realtimeEventPublisher =
                mock(KiosqueroRealtimeEventPublisher.class);
        CloseDailyBuffet useCase = new CloseDailyBuffet(
                expireAdvancePurchases,
                generateDailyBuffetReport,
                closeRepository,
                realtimeEventPublisher
        );
        DailyBuffetReport report = report(buffetId, date);
        DailyBuffetClose close = new DailyBuffetClose(
                UUID.randomUUID(),
                buffetId,
                date,
                2,
                4,
                BigDecimal.ZERO
        );

        when(closeRepository.findByBuffetAndDate(buffetId, date)).thenReturn(Optional.empty());
        when(expireAdvancePurchases.executeDetailed(any(), any()))
                .thenReturn(new ExpirationResult(2, 4));
        when(closeRepository.saveWithInventorySnapshot(any(DailyBuffetClose.class), anyList()))
                .thenReturn(close);
        when(generateDailyBuffetReport.execute(buffetId, date)).thenReturn(report);

        DailyCloseResult result = useCase.execute(buffetId, date);

        assertFalse(result.alreadyClosed());
        assertEquals(2, result.expiredPurchases());
        assertEquals(4, result.releasedReservations());
        verify(closeRepository).saveWithInventorySnapshot(
                any(DailyBuffetClose.class),
                eqSnapshot(report.inventory()));
        assertDailyReportChangedEvent(realtimeEventPublisher, buffetId, date);
    }

    @Test
    void execute_WhenCloseExists_ShouldNotExpireAgain() {
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 8);
        ExpireAdvancePurchases expireAdvancePurchases = mock(ExpireAdvancePurchases.class);
        GenerateDailyBuffetReport generateDailyBuffetReport =
                mock(GenerateDailyBuffetReport.class);
        DailyBuffetCloseRepository closeRepository = mock(DailyBuffetCloseRepository.class);
        CloseDailyBuffet useCase = new CloseDailyBuffet(
                expireAdvancePurchases,
                generateDailyBuffetReport,
                closeRepository
        );
        DailyBuffetClose close = new DailyBuffetClose(
                UUID.randomUUID(),
                buffetId,
                date,
                1,
                3,
                BigDecimal.ZERO
        );

        when(closeRepository.findByBuffetAndDate(buffetId, date))
                .thenReturn(Optional.of(close));
        when(generateDailyBuffetReport.execute(buffetId, date)).thenReturn(report(buffetId, date));

        DailyCloseResult result = useCase.execute(buffetId, date);

        assertTrue(result.alreadyClosed());
        assertEquals(1, result.expiredPurchases());
        verify(expireAdvancePurchases, never()).executeDetailed(any(), any());
    }

    private DailyBuffetReport report(UUID buffetId, LocalDate date) {
        return new DailyBuffetReport(
                buffetId,
                date,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                BigDecimal.ZERO,
                List.of(),
                List.of(snapshot())
        );
    }

    private List<DailyInventorySnapshot> eqSnapshot(List<DailyInventorySnapshot> snapshot) {
        return org.mockito.ArgumentMatchers.eq(snapshot);
    }

    private DailyInventorySnapshot snapshot() {
        return new DailyInventorySnapshot(
                UUID.randomUUID(),
                "Alfajor",
                10,
                1,
                9,
                3,
                null,
                null,
                EstadoInventario.DISPONIBLE,
                TipoManejoInventario.STOCK_EXACTO
        );
    }

    private void assertDailyReportChangedEvent(
            KiosqueroRealtimeEventPublisher realtimeEventPublisher,
            UUID buffetId,
            LocalDate date
    ) {
        ArgumentCaptor<KiosqueroRealtimeEvent> eventCaptor =
                ArgumentCaptor.forClass(KiosqueroRealtimeEvent.class);
        verify(realtimeEventPublisher).publish(eventCaptor.capture());
        assertEquals(KiosqueroRealtimeEventType.DAILY_REPORT_CHANGED,
                eventCaptor.getValue().type());
        assertEquals(buffetId, eventCaptor.getValue().buffetId());
        assertEquals(date, eventCaptor.getValue().date());
    }
}
