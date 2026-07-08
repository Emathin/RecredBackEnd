package com.recred.backend.unit.application;

import com.recred.backend.application.GetDailyBuffetCloseStatus;
import com.recred.backend.application.GetDailyBuffetCloseStatusRequest;
import com.recred.backend.application.GetDailyBuffetCloseStatusResponse;
import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;
import org.junit.jupiter.api.BeforeEach;
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

    private DailyBuffetCloseRepository repository;
    private GetDailyBuffetCloseStatus useCase;

    private UUID buffetId;
    private LocalDate date;
    private GetDailyBuffetCloseStatusResponse response;

    @BeforeEach
    void setUp() {
        repository = mock(DailyBuffetCloseRepository.class);
        useCase = new GetDailyBuffetCloseStatus(repository);
    }

    @Test
    void givenDailyBuffetAlreadyClosed_whenGettingCloseStatus_thenReturnsClosedStatus() {
        givenCloseStatusScenario();
        givenDailyBuffetCloseExists();

        whenGettingCloseStatus();

        thenDailyBuffetIsClosed();
        thenCloseCountersAreReturned();
    }

    @Test
    void givenDailyBuffetNotClosed_whenGettingCloseStatus_thenReturnsOpenStatus() {
        givenCloseStatusScenario();
        givenDailyBuffetCloseDoesNotExist();

        whenGettingCloseStatus();

        thenDailyBuffetIsOpen();
        thenOpenStatusHasZeroCounters();
    }

    private void givenCloseStatusScenario() {
        buffetId = UUID.randomUUID();
        date = LocalDate.of(2026, 6, 10);
    }

    private void givenDailyBuffetCloseExists() {
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

    private void givenDailyBuffetCloseDoesNotExist() {
        when(repository.findByBuffetAndDate(buffetId, date)).thenReturn(Optional.empty());
    }

    private void whenGettingCloseStatus() {
        response = useCase.execute(new GetDailyBuffetCloseStatusRequest(buffetId, date));
    }

    private void thenDailyBuffetIsClosed() {
        assertTrue(response.closed());
    }

    private void thenCloseCountersAreReturned() {
        assertEquals(2, response.expiredPurchases());
        assertEquals(5, response.releasedReservations());
        assertEquals(new BigDecimal("30.00"), response.refundedCredits());
    }

    private void thenDailyBuffetIsOpen() {
        assertFalse(response.closed());
        assertEquals(buffetId, response.buffetId());
        assertEquals(date, response.date());
    }

    private void thenOpenStatusHasZeroCounters() {
        assertEquals(0, response.expiredPurchases());
        assertEquals(0, response.releasedReservations());
        assertEquals(BigDecimal.ZERO, response.refundedCredits());
    }
}
