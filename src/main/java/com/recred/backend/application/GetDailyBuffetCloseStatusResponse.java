package com.recred.backend.application;

import com.recred.backend.domain.models.DailyBuffetClose;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GetDailyBuffetCloseStatusResponse(
        UUID buffetId,
        LocalDate date,
        boolean closed,
        int expiredPurchases,
        int releasedReservations,
        BigDecimal refundedCredits
) {
    public static GetDailyBuffetCloseStatusResponse closed(DailyBuffetClose close) {
        return new GetDailyBuffetCloseStatusResponse(
                close.buffetId(),
                close.date(),
                true,
                close.expiredPurchases(),
                close.releasedReservations(),
                close.refundedCredits()
        );
    }

    public static GetDailyBuffetCloseStatusResponse open(UUID buffetId, LocalDate date) {
        return new GetDailyBuffetCloseStatusResponse(
                buffetId,
                date,
                false,
                0,
                0,
                BigDecimal.ZERO
        );
    }
}
