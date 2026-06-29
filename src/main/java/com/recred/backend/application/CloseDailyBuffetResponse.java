package com.recred.backend.application;

import java.math.BigDecimal;

public record CloseDailyBuffetResponse(
        boolean alreadyClosed,
        int expiredPurchases,
        int releasedReservations,
        BigDecimal refundedCredits,
        GenerateDailyBuffetReportResponse report
) {
}
