package com.recred.backend.application;

import java.util.UUID;

public record TimeRestrictionCommand(
        UUID studentId,
        UUID timeSlotId,
        UUID categoryId,
        UUID classificationId
) {}