package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DailyBuffetClose(
        UUID id,
        UUID buffetId,
        LocalDate date,
        int expiredPurchases,
        int releasedReservations,
        BigDecimal refundedCredits
) {
}
