package com.recred.backend.application;

import java.time.LocalDateTime;

public record GenerateParentSummaryRequest(
        LocalDateTime start,
        LocalDateTime end
) {}
