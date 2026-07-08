package com.recred.backend.application;

import java.util.UUID;

public record DeactivateGradeRequest(
        UUID colegioId,
        UUID gradeId
) {
}
