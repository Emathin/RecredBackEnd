package com.recred.backend.domain.exceptions;

import java.time.LocalDate;
import java.util.UUID;

public class PickupSlotCapacityExceededException extends RuntimeException {

    public PickupSlotCapacityExceededException(UUID slotId, LocalDate pickupDate, int capacity) {
        super("La franja de retiro " + slotId + " no tiene cupo disponible para "
                + pickupDate + ". Cupo maximo: " + capacity);
    }
}
