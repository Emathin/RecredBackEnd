package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class TimeSlotInUseException extends RuntimeException {

    public TimeSlotInUseException(UUID slotId) {
        super("La franja horaria " + slotId
                + " tiene compras futuras activas y no puede modificarse");
    }
}
