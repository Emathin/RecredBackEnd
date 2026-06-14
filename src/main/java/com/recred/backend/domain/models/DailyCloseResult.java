package com.recred.backend.domain.models;

public record DailyCloseResult(
        boolean alreadyClosed,
        int expiredPurchases,
        int releasedReservations,
        java.math.BigDecimal refundedCredits,
        DailyBuffetReport report
) {
    public DailyCloseResult(int expiredPurchases, DailyBuffetReport report) {
        this(false, expiredPurchases, 0, java.math.BigDecimal.ZERO, report);
    }
}
