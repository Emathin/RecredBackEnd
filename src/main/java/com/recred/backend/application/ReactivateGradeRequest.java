package com.recred.backend.application;

import java.util.UUID;

public record ReactivateGradeRequest(
        UUID colegioId,
        UUID gradeId
) {
}
