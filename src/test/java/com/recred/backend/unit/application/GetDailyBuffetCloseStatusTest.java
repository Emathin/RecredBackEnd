package com.recred.backend.unit.application;

import com.recred.backend.application.GetDailyBuffetCloseStatus;
import com.recred.backend.application.GetDailyBuffetCloseStatusRequest;
import com.recred.backend.application.GetDailyBuffetCloseStatusResponse;
import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetDailyBuffetCloseStatusTest {

    private final DailyBuffetCloseRepository repository =
            mock(DailyBuffetCloseRepository.class);
    private final GetDailyBuffetCloseStatus useCase =
            new GetDailyBuffetCloseStatus(repository);
    private UUID buffetId;
    private LocalDate date;
    private GetDailyBuffetCloseStatusResponse response;

    @Test
    void execute_WhenCloseExists_ShouldReturnClosedStatus() {
        givenCloseDate();
        givenCloseExists();

        whenGettingCloseStatus();

        assertTrue(response.closed());
        assertEquals(2, response.expiredPurchases());
        assertEquals(5, response.releasedReservations());
        assertEquals(new BigDecimal("30.00"), response.refundedCredits());
    }

    @Test
    void execute_WhenCloseDoesNotExist_ShouldReturnOpenStatus() {
        givenCloseDate();
        givenCloseDoesNotExist();

        whenGettingCloseStatus();

        assertFalse(response.closed());
        assertEquals(buffetId, response.buffetId());
        assertEquals(date, response.date());
        assertEquals(0, response.expiredPurchases());
        assertEquals(0, response.releasedReservations());
        assertEquals(BigDecimal.ZERO, response.refundedCredits());
    }

    private void givenCloseDate() {
        buffetId = UUID.randomUUID();
        date = LocalDate.of(2026, 6, 10);
    }

    private void givenCloseExists() {
        DailyBuffetClose close = new DailyBuffetClose(
                UUID.randomUUID(),
                buffetId,
                date,
                2,
                5,
                new BigDecimal("30.00")
        );
        when(repository.findByBuffetAndDate(buffetId, date)).thenReturn(Optional.of(close));
    }

    private void givenCloseDoesNotExist() {
        when(repository.findByBuffetAndDate(buffetId, date)).thenReturn(Optional.empty());
    }

    private void whenGettingCloseStatus() {
        response = useCase.execute(new GetDailyBuffetCloseStatusRequest(buffetId, date));
    }
}
