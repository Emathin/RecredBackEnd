package com.recred.backend.application;

import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimeEventPublisher;
import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

public class CloseDailyBuffet {

    private final ExpireAdvancePurchases expireAdvancePurchases;
    private final GenerateDailyBuffetReport generateDailyBuffetReport;
    private final DailyBuffetCloseRepository dailyBuffetCloseRepository;
    private final KiosqueroRealtimeEventPublisher realtimeEventPublisher;

    public CloseDailyBuffet(
            ExpireAdvancePurchases expireAdvancePurchases,
            GenerateDailyBuffetReport generateDailyBuffetReport,
            DailyBuffetCloseRepository dailyBuffetCloseRepository
    ) {
        this(expireAdvancePurchases, generateDailyBuffetReport, dailyBuffetCloseRepository,
                event -> {
                });
    }

    public CloseDailyBuffet(
            ExpireAdvancePurchases expireAdvancePurchases,
            GenerateDailyBuffetReport generateDailyBuffetReport,
            DailyBuffetCloseRepository dailyBuffetCloseRepository,
            KiosqueroRealtimeEventPublisher realtimeEventPublisher
    ) {
        this.expireAdvancePurchases = expireAdvancePurchases;
        this.generateDailyBuffetReport = generateDailyBuffetReport;
        this.dailyBuffetCloseRepository = dailyBuffetCloseRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional
    public CloseDailyBuffetResponse execute(CloseDailyBuffetRequest closeRequest) {
        return findExistingClose(closeRequest)
                .map(this::alreadyClosedResponse)
                .orElseGet(() -> closeDailyBuffet(closeRequest));
    }

    private Optional<DailyBuffetClose> findExistingClose(CloseDailyBuffetRequest closeRequest) {
        return dailyBuffetCloseRepository.findByBuffetAndDate(
                closeRequest.buffetId(),
                closeRequest.date());
    }

    private CloseDailyBuffetResponse closeDailyBuffet(CloseDailyBuffetRequest closeRequest) {
        ExpireAdvancePurchasesResponse expiration = expirePurchasesUntilEndOfDay(closeRequest);
        GenerateDailyBuffetReportResponse report = generateDailyReport(closeRequest);
        DailyBuffetClose close = saveCloseWithInventorySnapshot(
                closeRequest,
                expiration,
                report);

        publishDailyReportChanged(close);
        return newCloseResponse(close, report);
    }

    private ExpireAdvancePurchasesResponse expirePurchasesUntilEndOfDay(
            CloseDailyBuffetRequest closeRequest) {
        return expireAdvancePurchases.execute(
                new ExpireAdvancePurchasesRequest(
                        closeRequest.buffetId(), closeRequest.date().atTime(LocalTime.MAX)
                )
        );
    }

    private GenerateDailyBuffetReportResponse generateDailyReport(
            CloseDailyBuffetRequest closeRequest) {
        return dailyReport(closeRequest.buffetId(), closeRequest.date());
    }

    private DailyBuffetClose saveCloseWithInventorySnapshot(
            CloseDailyBuffetRequest closeRequest,
            ExpireAdvancePurchasesResponse expiration,
            GenerateDailyBuffetReportResponse report) {
        return dailyBuffetCloseRepository.saveWithInventorySnapshot(
                new DailyBuffetClose(
                        null,
                        closeRequest.buffetId(),
                        closeRequest.date(),
                        expiration.expiredPurchases(),
                        expiration.releasedReservations(),
                        BigDecimal.ZERO
                ),
                report.inventorySnapshotDomain()
        );
    }

    private void publishDailyReportChanged(DailyBuffetClose close) {
        realtimeEventPublisher.publish(KiosqueroRealtimeEvent.dailyReportChanged(
                close.buffetId(),
                close.date(),
                "Cierre diario realizado"
        ));
    }

    private CloseDailyBuffetResponse alreadyClosedResponse(DailyBuffetClose close) {
        return closeResponse(
                close,
                true,
                dailyReport(close.buffetId(), close.date())
        );
    }

    private CloseDailyBuffetResponse newCloseResponse(
            DailyBuffetClose close,
            GenerateDailyBuffetReportResponse report) {
        return closeResponse(close, false, report);
    }

    private CloseDailyBuffetResponse closeResponse(
            DailyBuffetClose close,
            boolean alreadyClosed,
            GenerateDailyBuffetReportResponse report
    ) {
        return new CloseDailyBuffetResponse(
                alreadyClosed,
                close.expiredPurchases(),
                close.releasedReservations(),
                close.refundedCredits(),
                report
        );
    }

    private GenerateDailyBuffetReportResponse dailyReport(UUID buffetId, LocalDate date) {
        return generateDailyBuffetReport.execute(
                new GenerateDailyBuffetReportRequest(buffetId, date));
    }
}
