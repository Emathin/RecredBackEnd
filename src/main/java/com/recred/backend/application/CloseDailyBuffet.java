package com.recred.backend.application;

import com.recred.backend.application.ExpireAdvancePurchasesRequest;
import com.recred.backend.application.ExpireAdvancePurchasesResponse;
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
    public CloseDailyBuffetResponse execute(CloseDailyBuffetRequest request) {
        validate(request);
        return dailyBuffetCloseRepository.findByBuffetAndDate(request.buffetId(), request.date())
                .map(close -> result(close, true))
                .orElseGet(() -> close(request));
    }

    private void validate(CloseDailyBuffetRequest request) {
        required(request, "request is required");
        required(request.buffetId(), "buffetId is required");
        required(request.date(), "date is required");
    }

    private CloseDailyBuffetResponse close(CloseDailyBuffetRequest request) {
        ExpireAdvancePurchasesResponse expiration = expireAdvancePurchases.execute(
                new ExpireAdvancePurchasesRequest(
                        request.buffetId(), request.date().atTime(LocalTime.MAX)
                )
        );
        GenerateDailyBuffetReportResponse report = dailyReport(request.buffetId(), request.date());
        DailyBuffetClose close = dailyBuffetCloseRepository.saveWithInventorySnapshot(
                new DailyBuffetClose(
                        null,
                        request.buffetId(),
                        request.date(),
                        expiration.expiredPurchases(),
                        expiration.releasedReservations(),
                        BigDecimal.ZERO
                ),
                report.inventorySnapshotDomain()
        );
        publishDailyReportChanged(close);
        return result(close, false, report);
    }

    private void publishDailyReportChanged(DailyBuffetClose close) {
        realtimeEventPublisher.publish(KiosqueroRealtimeEvent.dailyReportChanged(
                close.buffetId(),
                close.date(),
                "Cierre diario realizado"
        ));
    }

    private CloseDailyBuffetResponse result(DailyBuffetClose close, boolean alreadyClosed) {
        return result(
                close,
                alreadyClosed,
                dailyReport(close.buffetId(), close.date())
        );
    }

    private CloseDailyBuffetResponse result(
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

    private <T> T required(T value, String message) {
        return Optional.ofNullable(value)
                .orElseThrow(() -> new IllegalArgumentException(message));
    }
}
