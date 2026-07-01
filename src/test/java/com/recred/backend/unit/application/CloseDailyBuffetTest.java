package com.recred.backend.unit.application;

import com.recred.backend.application.CloseDailyBuffet;
import com.recred.backend.application.CloseDailyBuffetRequest;
import com.recred.backend.application.CloseDailyBuffetResponse;
import com.recred.backend.application.ExpireAdvancePurchases;
import com.recred.backend.application.ExpireAdvancePurchasesRequest;
import com.recred.backend.application.ExpireAdvancePurchasesResponse;
import com.recred.backend.application.GenerateDailyBuffetReport;
import com.recred.backend.application.GenerateDailyBuffetReportRequest;
import com.recred.backend.application.GenerateDailyBuffetReportResponse;
import com.recred.backend.application.GenerateDailyBuffetReportResponse.DailyInventorySnapshotResponse;
import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimeEventPublisher;
import com.recred.backend.domain.events.KiosqueroRealtimeEventType;
import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.models.DailyInventorySnapshot;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloseDailyBuffetTest {

    private ExpireAdvancePurchases expireAdvancePurchases;
    private GenerateDailyBuffetReport generateDailyBuffetReport;
    private DailyBuffetCloseRepository closeRepository;
    private KiosqueroRealtimeEventPublisher realtimeEventPublisher;
    private CloseDailyBuffet useCase;
    private UUID buffetId;
    private LocalDate date;
    private DailyInventorySnapshot inventorySnapshot;
    private CloseDailyBuffetResponse response;

    @BeforeEach
    void setUp() {
        expireAdvancePurchases = mock(ExpireAdvancePurchases.class);
        generateDailyBuffetReport = mock(GenerateDailyBuffetReport.class);
        closeRepository = mock(DailyBuffetCloseRepository.class);
        realtimeEventPublisher = mock(KiosqueroRealtimeEventPublisher.class);
        useCase = new CloseDailyBuffet(
                expireAdvancePurchases,
                generateDailyBuffetReport,
                closeRepository,
                realtimeEventPublisher
        );
        buffetId = UUID.randomUUID();
        date = LocalDate.of(2026, 6, 8);
        inventorySnapshot = snapshot();
    }

    @Test
    void execute_WhenCloseDoesNotExist_ShouldExpireAndSaveClose() {
        DailyBuffetClose close = close(2, 4);
        givenCloseDoesNotExist();
        givenPurchasesWillExpire(2, 4);
        givenReportWillBeGenerated(List.of(inventorySnapshot));
        givenCloseWillBeSaved(close);

        whenClosingDailyBuffet();

        thenCloseIsReturnedAsNew(close);
        thenInventorySnapshotWasSaved();
        thenDailyReportChangedEventWasPublished();
    }

    @Test
    void execute_WhenCloseExists_ShouldNotExpireAgain() {
        DailyBuffetClose close = close(1, 3);
        givenCloseExists(close);
        givenReportWillBeGenerated(List.of(inventorySnapshot));

        whenClosingDailyBuffet();

        thenCloseIsReturnedAsAlreadyClosed(close);
        verify(expireAdvancePurchases, never()).execute(any(ExpireAdvancePurchasesRequest.class));
    }

    @Test
    void execute_WhenUsingDefaultPublisherAndCloseDoesNotExist_ShouldClose() {
        useCase = new CloseDailyBuffet(
                expireAdvancePurchases,
                generateDailyBuffetReport,
                closeRepository
        );
        DailyBuffetClose close = close(0, 0);
        givenCloseDoesNotExist();
        givenPurchasesWillExpire(0, 0);
        givenReportWillBeGenerated(List.of(inventorySnapshot));
        givenCloseWillBeSaved(close);

        whenClosingDailyBuffet();

        assertFalse(response.alreadyClosed());
        assertEquals(0, response.expiredPurchases());
        verify(closeRepository).saveWithInventorySnapshot(any(DailyBuffetClose.class), anyList());
    }

    private void givenCloseDoesNotExist() {
        when(closeRepository.findByBuffetAndDate(buffetId, date)).thenReturn(Optional.empty());
    }

    private void givenCloseExists(DailyBuffetClose close) {
        when(closeRepository.findByBuffetAndDate(buffetId, date))
                .thenReturn(Optional.of(close));
    }

    private void givenPurchasesWillExpire(int expiredPurchases, int releasedReservations) {
        when(expireAdvancePurchases.execute(any(ExpireAdvancePurchasesRequest.class)))
                .thenReturn(new ExpireAdvancePurchasesResponse(expiredPurchases, releasedReservations));
    }

    private void givenReportWillBeGenerated(List<DailyInventorySnapshot> inventory) {
        when(generateDailyBuffetReport.execute(new GenerateDailyBuffetReportRequest(
                buffetId,
                date
        ))).thenReturn(report(buffetId, date, inventory));
    }

    private void givenCloseWillBeSaved(DailyBuffetClose close) {
        when(closeRepository.saveWithInventorySnapshot(any(DailyBuffetClose.class), anyList()))
                .thenReturn(close);
    }

    private void whenClosingDailyBuffet() {
        response = useCase.execute(new CloseDailyBuffetRequest(buffetId, date));
    }

    private void thenCloseIsReturnedAsNew(DailyBuffetClose close) {
        assertFalse(response.alreadyClosed());
        assertEquals(close.expiredPurchases(), response.expiredPurchases());
        assertEquals(close.releasedReservations(), response.releasedReservations());
    }

    private void thenCloseIsReturnedAsAlreadyClosed(DailyBuffetClose close) {
        assertTrue(response.alreadyClosed());
        assertEquals(close.expiredPurchases(), response.expiredPurchases());
        assertEquals(close.releasedReservations(), response.releasedReservations());
    }

    private void thenInventorySnapshotWasSaved() {
        verify(closeRepository).saveWithInventorySnapshot(
                any(DailyBuffetClose.class),
                eq(List.of(inventorySnapshot)));
    }

    private void thenDailyReportChangedEventWasPublished() {
        ArgumentCaptor<KiosqueroRealtimeEvent> eventCaptor =
                ArgumentCaptor.forClass(KiosqueroRealtimeEvent.class);
        verify(realtimeEventPublisher).publish(eventCaptor.capture());
        assertEquals(KiosqueroRealtimeEventType.DAILY_REPORT_CHANGED,
                eventCaptor.getValue().type());
        assertEquals(buffetId, eventCaptor.getValue().buffetId());
        assertEquals(date, eventCaptor.getValue().date());
    }

    private DailyBuffetClose close(int expiredPurchases, int releasedReservations) {
        return new DailyBuffetClose(
                UUID.randomUUID(),
                buffetId,
                date,
                expiredPurchases,
                releasedReservations,
                BigDecimal.ZERO
        );
    }

    private GenerateDailyBuffetReportResponse report(
            UUID buffetId,
            LocalDate date,
            List<DailyInventorySnapshot> inventory
    ) {
        return new GenerateDailyBuffetReportResponse(
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
                BigDecimal.ZERO,
                0,
                List.of(),
                inventory.stream().map(this::snapshotResponse).toList(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private DailyInventorySnapshotResponse snapshotResponse(DailyInventorySnapshot snapshot) {
        return new DailyInventorySnapshotResponse(
                snapshot.productId(),
                snapshot.productName(),
                snapshot.stockActual(),
                snapshot.stockReservado(),
                snapshot.stockDisponible(),
                snapshot.stockMinimo(),
                snapshot.cupoMaximoDiario(),
                snapshot.cupoDisponibleDia(),
                snapshot.estadoInventario(),
                snapshot.tipoManejoInventario()
        );
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
}
