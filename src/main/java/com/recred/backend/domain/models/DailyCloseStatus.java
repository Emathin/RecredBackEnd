package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DailyCloseStatus(
        UUID buffetId,
        LocalDate date,
        boolean closed,
        int expiredPurchases,
        int releasedReservations,
        BigDecimal refundedCredits
) {
    public static DailyCloseStatus closed(DailyBuffetClose close) {
        return new DailyCloseStatus(
                close.buffetId(),
                close.date(),
                true,
                close.expiredPurchases(),
                close.releasedReservations(),
                close.refundedCredits()
        );
    }

    public static DailyCloseStatus open(UUID buffetId, LocalDate date) {
        return new DailyCloseStatus(
                buffetId,
                date,
                false,
                0,
                0,
                BigDecimal.ZERO
        );
    }
}
