package com.recred.backend.application;

import java.util.UUID;

public record ReactivateTimeSlotRequest(
        UUID colegioId,
        UUID slotId
) {
}
