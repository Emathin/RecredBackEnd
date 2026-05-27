package com.recred.backend.domain.models;

import java.util.List;
import java.util.UUID;

public record ConsumptionAlertCandidate(
        UUID studentId,
        UUID studentUserId,
        String studentName,
        List<ConsumptionAlertTutor> tutors
) {
}
