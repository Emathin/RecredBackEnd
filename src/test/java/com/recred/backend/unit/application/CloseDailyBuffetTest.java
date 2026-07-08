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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloseDailyBuffetTest {

    private static final LocalDate CLOSE_DATE = LocalDate.of(2026, 6, 8);

    @Mock
    private ExpireAdvancePurchases expireAdvancePurchases;
    @Mock
    private GenerateDailyBuffetReport generateDailyBuffetReport;
    @Mock
    private DailyBuffetCloseRepository closeRepository;
    @Mock
    private KiosqueroRealtimeEventPublisher realtimeEventPublisher;
    @Captor
    private ArgumentCaptor<KiosqueroRealtimeEvent> eventCaptor;

    private CloseDailyBuffet useCase;
    private UUID buffetId;
    private DailyInventorySnapshot inventorySnapshot;
    private DailyBuffetClose close;
    private CloseDailyBuffetResponse response;

    @BeforeEach
    void setUp() {
        buffetId = UUID.randomUUID();
        inventorySnapshot = inventorySnapshot();
        useCase = new CloseDailyBuffet(
                expireAdvancePurchases,
                generateDailyBuffetReport,
                closeRepository,
                realtimeEventPublisher
        );
    }

    @Test
    void openBusinessDayCanBeClosedWithInventorySnapshotAndRealtimeEvent() {
        givenBusinessDayWasNotClosedYet();
        givenAdvancePurchasesExpire(2, 4);
        givenDailyReportCanBeGenerated();
        givenCloseWillBeSavedWith(2, 4);

        whenClosingDailyBuffet();

        thenCloseIsReturnedAsNew();
        thenInventorySnapshotWasSaved();
        thenDailyReportChangedEventWasPublished();
    }

    @Test
    void alreadyClosedBusinessDayReturnsExistingCloseWithoutExpiringAgain() {
        givenBusinessDayWasAlreadyClosed(1, 3);
        givenDailyReportCanBeGenerated();

        whenClosingDailyBuffet();

        thenCloseIsReturnedAsAlreadyClosed();
        thenAdvancePurchasesWereNotExpiredAgain();
    }

    @Test
    void defaultPublisherKeepsCloseFlowAvailable() {
        useCase = new CloseDailyBuffet(
                expireAdvancePurchases,
                generateDailyBuffetReport,
                closeRepository
        );
        givenBusinessDayWasNotClosedYet();
        givenAdvancePurchasesExpire(0, 0);
        givenDailyReportCanBeGenerated();
        givenCloseWillBeSavedWith(0, 0);

        whenClosingDailyBuffet();

        thenCloseIsReturnedAsNew();
        thenInventorySnapshotWasSaved();
    }

    private void givenBusinessDayWasNotClosedYet() {
        when(closeRepository.findByBuffetAndDate(buffetId, CLOSE_DATE))
                .thenReturn(Optional.empty());
    }

    private void givenBusinessDayWasAlreadyClosed(
            int expiredPurchases,
            int releasedReservations) {
        close = close(expiredPurchases, releasedReservations);
        when(closeRepository.findByBuffetAndDate(buffetId, CLOSE_DATE))
                .thenReturn(Optional.of(close));
    }

    private void givenAdvancePurchasesExpire(
            int expiredPurchases,
            int releasedReservations) {
        when(expireAdvancePurchases.execute(any(ExpireAdvancePurchasesRequest.class)))
                .thenReturn(new ExpireAdvancePurchasesResponse(
                        expiredPurchases,
                        releasedReservations));
    }

    private void givenDailyReportCanBeGenerated() {
        when(generateDailyBuffetReport.execute(
                new GenerateDailyBuffetReportRequest(buffetId, CLOSE_DATE)))
                .thenReturn(report(List.of(inventorySnapshot)));
    }

    private void givenCloseWillBeSavedWith(int expiredPurchases, int releasedReservations) {
        close = close(expiredPurchases, releasedReservations);
        when(closeRepository.saveWithInventorySnapshot(any(DailyBuffetClose.class), anyList()))
                .thenReturn(close);
    }

    private void whenClosingDailyBuffet() {
        response = useCase.execute(defaultRequest());
    }

    private void thenCloseIsReturnedAsNew() {
        assertFalse(response.alreadyClosed());
        assertEquals(close.expiredPurchases(), response.expiredPurchases());
        assertEquals(close.releasedReservations(), response.releasedReservations());
    }

    private void thenCloseIsReturnedAsAlreadyClosed() {
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
        verify(realtimeEventPublisher).publish(eventCaptor.capture());
        KiosqueroRealtimeEvent event = eventCaptor.getValue();
        assertEquals(KiosqueroRealtimeEventType.DAILY_REPORT_CHANGED, event.type());
        assertEquals(buffetId, event.buffetId());
        assertEquals(CLOSE_DATE, event.date());
    }

    private void thenAdvancePurchasesWereNotExpiredAgain() {
        verify(expireAdvancePurchases, never()).execute(any(ExpireAdvancePurchasesRequest.class));
    }

    private CloseDailyBuffetRequest defaultRequest() {
        return new CloseDailyBuffetRequest(buffetId, CLOSE_DATE);
    }

    private DailyBuffetClose close(int expiredPurchases, int releasedReservations) {
        return new DailyBuffetClose(
                UUID.randomUUID(),
                buffetId,
                CLOSE_DATE,
                expiredPurchases,
                releasedReservations,
                BigDecimal.ZERO
        );
    }

    private GenerateDailyBuffetReportResponse report(
            List<DailyInventorySnapshot> inventory) {
        return new GenerateDailyBuffetReportResponse(
                buffetId,
                CLOSE_DATE,
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

    private DailyInventorySnapshot inventorySnapshot() {
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
