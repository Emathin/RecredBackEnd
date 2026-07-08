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
class GetLastSummaryTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SUMMARY_ID = UUID.randomUUID();
    private static final String JSON_RESUMEN = "{}";

    @Mock
    private SummaryRepository summaryRepository;

    @InjectMocks
    private GetLastSummary getLastSummary;

    @Test
    void executeReturnsResponseWhenSummaryFound() {
        Summary summary = buildSummary();
        givenSummaryFound(summary);

        Optional<GetLastSummaryResponse> response = whenExecute();

        thenResponseMatches(response, summary);
    }

    @Test
    void executeReturnsEmptyWhenNoSummaryFound() {
        givenNoSummaryFound();

        Optional<GetLastSummaryResponse> response = whenExecute();

        thenResponseIsEmpty(response);
    }

    private Summary buildSummary() {
        Summary summary = new Summary();
        summary.setId(SUMMARY_ID);
        summary.setFechaDesde(LocalDateTime.now().minusDays(7));
        summary.setFechaHasta(LocalDateTime.now());
        summary.setResumen(JSON_RESUMEN);
        return summary;
    }

    private void givenSummaryFound(Summary summary) {
        when(summaryRepository.findLastByUsuarioId(USER_ID)).thenReturn(Optional.of(summary));
    }

    private void givenNoSummaryFound() {
        when(summaryRepository.findLastByUsuarioId(USER_ID)).thenReturn(Optional.empty());
    }

    private Optional<GetLastSummaryResponse> whenExecute() {
        return getLastSummary.execute(new GetLastSummaryRequest(USER_ID));
    }

    private void thenResponseMatches(Optional<GetLastSummaryResponse> response, Summary summary) {
        assertTrue(response.isPresent());
        assertEquals(summary.getId(), response.get().id());
        assertEquals(summary.getResumen(), response.get().resumen());
    }

    private void thenResponseIsEmpty(Optional<GetLastSummaryResponse> response) {
        assertTrue(response.isEmpty());
    }
}
