package com.recred.backend.application;

import com.recred.backend.domain.models.Summary;
import com.recred.backend.domain.repositories.SummaryRepository;

import java.util.Optional;

public class GetLastSummary {

    private final SummaryRepository summaryRepository;

    public GetLastSummary(SummaryRepository summaryRepository) {
        this.summaryRepository = summaryRepository;
    }

    public Optional<GetLastSummaryResponse> execute(GetLastSummaryRequest request) {
        return fetchSummary(request)
                .map(this::mapToResponse);
    }

    private Optional<Summary> fetchSummary(GetLastSummaryRequest request) {
        return summaryRepository.findLastByUsuarioId(request.userId());
    }

    private GetLastSummaryResponse mapToResponse(Summary summary) {
        return GetLastSummaryResponse.fromSummary(summary);
    }
}
