package com.recred.backend.application;

import java.util.List;
import java.util.UUID;

public record GenerateParentSummaryResponse(
        List<UUID> processedUserIds
) {}
