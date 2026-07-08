package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.models.DailyCloseStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyCloseStatusTest {

    @Test
    void closedShouldMapDailyCloseValues() {
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 4);

        DailyCloseStatus status = DailyCloseStatus.closed(new DailyBuffetClose(
                UUID.randomUUID(),
                buffetId,
                date,
                4,
                3,
                new BigDecimal("1200.00")
        ));

        assertEquals(buffetId, status.buffetId());
        assertEquals(date, status.date());
        assertTrue(status.closed());
        assertEquals(4, status.expiredPurchases());
        assertEquals(3, status.releasedReservations());
        assertEquals(new BigDecimal("1200.00"), status.refundedCredits());
    }

    @Test
    void openShouldCreateOpenStatusWithZeroCounters() {
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 5);

        DailyCloseStatus status = DailyCloseStatus.open(buffetId, date);

        assertEquals(buffetId, status.buffetId());
        assertEquals(date, status.date());
        assertFalse(status.closed());
        assertEquals(0, status.expiredPurchases());
        assertEquals(0, status.releasedReservations());
        assertEquals(BigDecimal.ZERO, status.refundedCredits());
    }
}
