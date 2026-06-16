package com.recred.backend.unit.application;

import com.recred.backend.application.GetDailyBuffetCloseStatus;
import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.models.DailyCloseStatus;
import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetDailyBuffetCloseStatusTest {

    @Test
    void execute_WhenCloseExists_ShouldReturnClosedStatus() {
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 10);
        DailyBuffetClose close = new DailyBuffetClose(
                UUID.randomUUID(),
                buffetId,
                date,
                2,
                5,
                new BigDecimal("30.00")
        );
        DailyBuffetCloseRepository repository = mock(DailyBuffetCloseRepository.class);
        GetDailyBuffetCloseStatus useCase = new GetDailyBuffetCloseStatus(repository);

        when(repository.findByBuffetAndDate(buffetId, date)).thenReturn(Optional.of(close));

        DailyCloseStatus result = useCase.execute(buffetId, date);

        assertTrue(result.closed());
        assertEquals(2, result.expiredPurchases());
        assertEquals(5, result.releasedReservations());
        assertEquals(new BigDecimal("30.00"), result.refundedCredits());
    }

    @Test
    void execute_WhenCloseDoesNotExist_ShouldReturnOpenStatus() {
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 10);
        DailyBuffetCloseRepository repository = mock(DailyBuffetCloseRepository.class);
        GetDailyBuffetCloseStatus useCase = new GetDailyBuffetCloseStatus(repository);

        when(repository.findByBuffetAndDate(buffetId, date)).thenReturn(Optional.empty());

        DailyCloseStatus result = useCase.execute(buffetId, date);

        assertFalse(result.closed());
        assertEquals(buffetId, result.buffetId());
        assertEquals(date, result.date());
        assertEquals(0, result.expiredPurchases());
        assertEquals(0, result.releasedReservations());
        assertEquals(BigDecimal.ZERO, result.refundedCredits());
    }
}
