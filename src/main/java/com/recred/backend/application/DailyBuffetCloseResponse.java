package com.recred.backend.application;

import com.recred.backend.domain.models.DailyBuffetClose;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DailyBuffetCloseResponse(
        UUID id,
        UUID buffetId,
        LocalDate date,
        int expiredPurchases,
        int releasedReservations,
        BigDecimal refundedCredits
) {
    public static DailyBuffetCloseResponse fromDomain(DailyBuffetClose close) {
        return new DailyBuffetCloseResponse(
                close.id(),
                close.buffetId(),
                close.date(),
                close.expiredPurchases(),
                close.releasedReservations(),
                close.refundedCredits()
        );
    }
}
