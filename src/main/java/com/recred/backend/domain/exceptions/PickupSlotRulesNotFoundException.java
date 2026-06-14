package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class PickupSlotRulesNotFoundException extends RuntimeException {

    public PickupSlotRulesNotFoundException(UUID slotId) {
        super("No se encontro la franja horaria: " + slotId);
    }
}
