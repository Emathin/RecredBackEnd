package com.recred.backend.domain.models;

import java.util.List;

public record ProductChangeAnalysis (
        String summary,
        List<String> alerts,
        List<String> recommendations,
        String model
) {
}
