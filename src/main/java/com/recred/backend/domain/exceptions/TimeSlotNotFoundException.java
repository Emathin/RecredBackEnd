package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class TimeSlotNotFoundException extends RuntimeException {

    public TimeSlotNotFoundException(UUID slotId) {
        super("No se encontro la franja horaria: " + slotId);
    }
}
