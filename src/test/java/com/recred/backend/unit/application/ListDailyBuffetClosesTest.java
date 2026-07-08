package com.recred.backend.unit.application;

import com.recred.backend.application.ListDailyBuffetCloses;
import com.recred.backend.application.ListDailyBuffetClosesRequest;
import com.recred.backend.application.ListDailyBuffetClosesResponse;
import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListDailyBuffetClosesTest {

    private static final UUID BUFFET_ID = UUID.randomUUID();
    private static final LocalDate START_DATE = LocalDate.of(2026, 6, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 6, 10);
    private static final UUID CLOSE_ID = UUID.randomUUID();

    @Mock
    private DailyBuffetCloseRepository repository;

    @InjectMocks
    private ListDailyBuffetCloses useCase;

    @Test
    void shouldReturnClosesInRange() {
        givenRepositoryHasCloses();

        ListDailyBuffetClosesResponse response = whenListingCloses();

        thenClosesAreListedCorrectly(response);
    }

    private void givenRepositoryHasCloses() {
        DailyBuffetClose close = new DailyBuffetClose(
                CLOSE_ID,
                BUFFET_ID,
                LocalDate.of(2026, 6, 8),
                1,
                2,
                BigDecimal.ZERO
        );
        when(repository.findByBuffetAndDateRange(BUFFET_ID, START_DATE, END_DATE))
                .thenReturn(List.of(close));
    }

    private ListDailyBuffetClosesResponse whenListingCloses() {
        return useCase.execute(new ListDailyBuffetClosesRequest(BUFFET_ID, START_DATE, END_DATE));
    }

    private void thenClosesAreListedCorrectly(ListDailyBuffetClosesResponse response) {
        assertEquals(1, response.closes().size());
        assertEquals(CLOSE_ID, response.closes().get(0).id());
        verify(repository).findByBuffetAndDateRange(BUFFET_ID, START_DATE, END_DATE);
    }
}
