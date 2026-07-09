package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class TimeSlotOverlapException extends RuntimeException {

    public TimeSlotOverlapException(UUID schoolId) {
        super("Ya existe una franja horaria activa superpuesta");
    }
}
