package com.recred.backend.unit.application;

import com.recred.backend.application.ListDailyBuffetCloses;
import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListDailyBuffetClosesTest {

    @Test
    void execute_ShouldReturnClosesInRange() {
        UUID buffetId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 6, 1);
        LocalDate endDate = LocalDate.of(2026, 6, 10);
        DailyBuffetClose close = new DailyBuffetClose(
                UUID.randomUUID(),
                buffetId,
                LocalDate.of(2026, 6, 8),
                1,
                2,
                BigDecimal.ZERO
        );
        DailyBuffetCloseRepository repository = mock(DailyBuffetCloseRepository.class);
        ListDailyBuffetCloses useCase = new ListDailyBuffetCloses(repository);

        when(repository.findByBuffetAndDateRange(buffetId, startDate, endDate))
                .thenReturn(List.of(close));

        List<DailyBuffetClose> result = useCase.execute(buffetId, startDate, endDate);

        assertEquals(List.of(close), result);
        verify(repository).findByBuffetAndDateRange(buffetId, startDate, endDate);
    }

    @Test
    void execute_WhenRangeIsInvalid_ShouldFail() {
        DailyBuffetCloseRepository repository = mock(DailyBuffetCloseRepository.class);
        ListDailyBuffetCloses useCase = new ListDailyBuffetCloses(repository);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                UUID.randomUUID(),
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 1)
        ));
        verifyNoInteractions(repository);
    }
}
