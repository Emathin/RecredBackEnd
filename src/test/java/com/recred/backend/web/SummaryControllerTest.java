package com.recred.backend.web;

import com.recred.backend.application.GenerateParentSummary;
import com.recred.backend.domain.models.Summary;
import com.recred.backend.domain.repositories.SummaryRepository;
import com.recred.backend.infrastructure.web.SummaryController;
import com.recred.backend.infrastructure.web.SummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SummaryControllerTest {

    private GenerateParentSummary generateParentSummary;
    private SummaryRepository summaryRepository;
    private SummaryController controller;

    @BeforeEach
    void setUp() {
        generateParentSummary = mock(GenerateParentSummary.class);
        summaryRepository = mock(SummaryRepository.class);
        controller = new SummaryController(generateParentSummary, summaryRepository);
    }

    @Test
    void getLastSummaryReturnsMappedSummaryWhenFound() {
        UUID userId = UUID.randomUUID();
        Summary summary = summary(UUID.randomUUID());
        when(summaryRepository.findLastByUsuarioId(userId)).thenReturn(Optional.of(summary));

        ResponseEntity<SummaryResponse> response = controller.getLastSummary(userId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(summary.getId().toString(), response.getBody().getId());
        assertEquals("01/06/26", response.getBody().getFechaDesde());
        assertEquals("Resumen semanal", response.getBody().getResumen());
    }

    @Test
    void getLastSummaryReturnsNotFoundWhenMissing() {
        UUID userId = UUID.randomUUID();
        when(summaryRepository.findLastByUsuarioId(userId)).thenReturn(Optional.empty());

        ResponseEntity<SummaryResponse> response = controller.getLastSummary(userId);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void getAllSummariesReturnsMappedList() {
        UUID userId = UUID.randomUUID();
        Summary summary = summary(UUID.randomUUID());
        when(summaryRepository.findAllByUsuarioId(userId)).thenReturn(List.of(summary));

        ResponseEntity<List<SummaryResponse>> response =
                controller.getAllSummaries(userId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(summary.getId().toString(), response.getBody().get(0).getId());
    }

    @Test
    void createSummaryGeneratesLastSevenDaysSummary() {
        UUID userId = UUID.randomUUID();
        Summary summary = summary(UUID.randomUUID());
        when(generateParentSummary.execute(eq(userId), any(), any())).thenReturn(summary);

        ResponseEntity<SummaryResponse> response = controller.createSummary(userId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Resumen semanal", response.getBody().getResumen());
        verify(generateParentSummary).execute(eq(userId), any(), any());
    }

    @Test
    void createCustomSummaryUsesProvidedDates() {
        UUID userId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 8, 0, 0);
        Summary summary = summary(UUID.randomUUID());
        when(generateParentSummary.execute(userId, start, end)).thenReturn(summary);

        ResponseEntity<SummaryResponse> response =
                controller.createCustomSummary(userId, start, end);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(summary.getId().toString(), response.getBody().getId());
        verify(generateParentSummary).execute(userId, start, end);
    }

    private Summary summary(UUID summaryId) {
        Summary summary = new Summary();
        summary.setId(summaryId);
        summary.setFechaDesde(LocalDateTime.of(2026, 6, 1, 0, 0));
        summary.setFechaHasta(LocalDateTime.of(2026, 6, 8, 0, 0));
        summary.setResumen("Resumen semanal");
        return summary;
    }
}
