package com.recred.backend.application;

import com.recred.backend.application.ExpireAdvancePurchases.ExpirationResult;
import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimeEventPublisher;
import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.models.DailyBuffetReport;
import com.recred.backend.domain.models.DailyCloseResult;
import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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
    public DailyCloseResult execute(UUID buffetId, LocalDate date) {
        return dailyBuffetCloseRepository.findByBuffetAndDate(buffetId, date)
                .map(close -> result(close, true))
                .orElseGet(() -> close(buffetId, date));
    }

    private DailyCloseResult close(UUID buffetId, LocalDate date) {
        ExpirationResult expiration = expireAdvancePurchases.executeDetailed(
                buffetId,
                date.atTime(LocalTime.MAX));
        DailyBuffetReport report = generateDailyBuffetReport.execute(buffetId, date);
        DailyBuffetClose close = dailyBuffetCloseRepository.saveWithInventorySnapshot(
                new DailyBuffetClose(
                        null,
                        buffetId,
                        date,
                        expiration.expiredPurchases(),
                        expiration.releasedReservations(),
                        BigDecimal.ZERO
                ),
                report.inventory()
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

    private DailyCloseResult result(DailyBuffetClose close, boolean alreadyClosed) {
        return result(
                close,
                alreadyClosed,
                generateDailyBuffetReport.execute(close.buffetId(), close.date())
        );
    }

    private DailyCloseResult result(
            DailyBuffetClose close,
            boolean alreadyClosed,
            DailyBuffetReport report
    ) {
        return new DailyCloseResult(
                alreadyClosed,
                close.expiredPurchases(),
                close.releasedReservations(),
                close.refundedCredits(),
                report
        );
    }
}
