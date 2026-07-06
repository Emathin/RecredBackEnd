package com.recred.backend.application;

import java.util.UUID;

public record ListSchoolTimeSlotsRequest(
        UUID colegioId,
        boolean includeInactive
) {
}
