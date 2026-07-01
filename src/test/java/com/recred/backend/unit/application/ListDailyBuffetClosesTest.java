package com.recred.backend.unit.application;

import com.recred.backend.application.ListDailyBuffetCloses;
import com.recred.backend.application.ListDailyBuffetClosesRequest;
import com.recred.backend.application.ListDailyBuffetClosesResponse;
import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListDailyBuffetClosesTest {

    private final DailyBuffetCloseRepository repository =
            mock(DailyBuffetCloseRepository.class);
    private final ListDailyBuffetCloses useCase = new ListDailyBuffetCloses(repository);
    private UUID buffetId;
    private LocalDate startDate;
    private LocalDate endDate;
    private DailyBuffetClose close;
    private ListDailyBuffetClosesResponse response;

    @Test
    void execute_ShouldReturnClosesInRange() {
        givenCloseRange();
        givenRepositoryHasCloses();

        whenListingCloses();

        assertEquals(1, response.closes().size());
        assertEquals(close.id(), response.closes().get(0).id());
        verify(repository).findByBuffetAndDateRange(buffetId, startDate, endDate);
    }

    @Test
    void execute_WhenRangeIsInvalid_ShouldFail() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                new ListDailyBuffetClosesRequest(
                        UUID.randomUUID(),
                        LocalDate.of(2026, 6, 10),
                        LocalDate.of(2026, 6, 1)
                )
        ));
        verifyNoInteractions(repository);
    }

    private void givenCloseRange() {
        buffetId = UUID.randomUUID();
        startDate = LocalDate.of(2026, 6, 1);
        endDate = LocalDate.of(2026, 6, 10);
    }

    private void givenRepositoryHasCloses() {
        close = new DailyBuffetClose(
                UUID.randomUUID(),
                buffetId,
                LocalDate.of(2026, 6, 8),
                1,
                2,
                BigDecimal.ZERO
        );
        when(repository.findByBuffetAndDateRange(buffetId, startDate, endDate))
                .thenReturn(List.of(close));
    }

    private void whenListingCloses() {
        response = useCase.execute(new ListDailyBuffetClosesRequest(
                buffetId,
                startDate,
                endDate
        ));
    }
}
