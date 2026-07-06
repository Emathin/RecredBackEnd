package com.recred.backend.application;

import java.util.UUID;

public record GetTimeSlotRequest(
        UUID colegioId,
        UUID slotId
) {
}
