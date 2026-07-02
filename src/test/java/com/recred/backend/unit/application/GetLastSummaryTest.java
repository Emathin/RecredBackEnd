package com.recred.backend.unit.application;

import com.recred.backend.application.GetLastSummary;
import com.recred.backend.application.GetLastSummaryRequest;
import com.recred.backend.application.GetLastSummaryResponse;
import com.recred.backend.domain.models.Summary;
import com.recred.backend.domain.repositories.SummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetLastSummaryTest {

    @Mock
    private SummaryRepository summaryRepository;

    @InjectMocks
    private GetLastSummary getLastSummary;

    @Test
    void executeReturnsResponseWhenSummaryFound() {
        UUID userId = UUID.randomUUID();
        Summary summary = new Summary();
        summary.setId(UUID.randomUUID());
        summary.setFechaDesde(LocalDateTime.now().minusDays(7));
        summary.setFechaHasta(LocalDateTime.now());
        summary.setResumen("{}");

        when(summaryRepository.findLastByUsuarioId(userId)).thenReturn(Optional.of(summary));

        Optional<GetLastSummaryResponse> response = getLastSummary.execute(new GetLastSummaryRequest(userId));

        assertTrue(response.isPresent());
        assertEquals(summary.getId(), response.get().id());
        assertEquals(summary.getResumen(), response.get().resumen());
    }

    @Test
    void executeReturnsEmptyWhenNoSummaryFound() {
        UUID userId = UUID.randomUUID();

        when(summaryRepository.findLastByUsuarioId(userId)).thenReturn(Optional.empty());

        Optional<GetLastSummaryResponse> response = getLastSummary.execute(new GetLastSummaryRequest(userId));

        assertTrue(response.isEmpty());
    }
}
