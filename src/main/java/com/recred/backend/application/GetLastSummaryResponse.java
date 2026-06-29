package com.recred.backend.application;

import com.recred.backend.domain.models.Summary;
import java.time.LocalDateTime;
import java.util.UUID;

public record GetLastSummaryResponse(
        UUID id,
        LocalDateTime fechaDesde,
        LocalDateTime fechaHasta,
        String resumen
) {
    public static GetLastSummaryResponse fromSummary(Summary summary) {
        return new GetLastSummaryResponse(
                summary.getId(),
                summary.getFechaDesde(),
                summary.getFechaHasta(),
                summary.getResumen()
        );
    }
}
