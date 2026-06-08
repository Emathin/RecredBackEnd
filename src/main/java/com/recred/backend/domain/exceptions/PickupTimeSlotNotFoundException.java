package com.recred.backend.domain.exceptions;

import com.recred.backend.domain.models.RecessTime;

import java.util.UUID;

public class PickupTimeSlotNotFoundException extends RuntimeException {

    public PickupTimeSlotNotFoundException(UUID schoolId, RecessTime recessTime) {
        super("No se encontro una franja horaria para " + recessTime
                + " en el colegio " + schoolId);
    }
}
