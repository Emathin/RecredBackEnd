package com.recred.backend.application;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public record StudentConsumptionCommand(
        Optional<UUID> studentId,
        Optional<LocalDate> from,
        Optional<LocalDate> to
) {
}
