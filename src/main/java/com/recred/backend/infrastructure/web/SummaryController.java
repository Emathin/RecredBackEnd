package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GenerateParentSummary;
import com.recred.backend.domain.models.Summary;
import com.recred.backend.domain.repositories.SummaryRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resumen")
public class SummaryController {

    private final GenerateParentSummary generateParentSummary;
    private final SummaryRepository summaryRepository;

    public SummaryController(GenerateParentSummary generateParentSummary,
                             SummaryRepository summaryRepository) {
        this.generateParentSummary = generateParentSummary;
        this.summaryRepository = summaryRepository;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<SummaryResponse> getLastSummary(@PathVariable UUID userId) {
        return summaryRepository.findLastByUsuarioId(userId)
                .map(SummaryResponse::new)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{userId}")
    public ResponseEntity<SummaryResponse> createSummary(@PathVariable UUID userId) {
        LocalDateTime fin = LocalDateTime.now();
        LocalDateTime inicio = fin.minusDays(7);
        Summary summary = generateParentSummary.execute(userId, inicio, fin);
        return ResponseEntity.ok(new SummaryResponse(summary));
    }

    @PostMapping("/{userId}/custom")
    public ResponseEntity<SummaryResponse> createCustomSummary(
            @PathVariable UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fin) {
        Summary summary = generateParentSummary.execute(userId, inicio, fin);
        return ResponseEntity.ok(new SummaryResponse(summary));
    }
}
