package com.recred.backend.domain.exceptions;

import java.time.LocalDateTime;
import java.util.UUID;

public class PickupCutoffTimeExceededException extends RuntimeException {

    public PickupCutoffTimeExceededException(UUID slotId, LocalDateTime cutoffTime) {
        super("La franja de retiro " + slotId + " ya cerro para compras anticipadas. "
                + "Horario de corte: " + cutoffTime);
    }
}
