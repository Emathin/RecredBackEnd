package com.recred.backend.application;

import java.util.UUID;

public record GetGradeRequest(
        UUID colegioId,
        UUID gradeId
) {
}
