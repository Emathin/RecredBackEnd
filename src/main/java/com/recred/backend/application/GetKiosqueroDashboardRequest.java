package com.recred.backend.application;

import java.time.LocalDate;
import java.util.UUID;

public record GetKiosqueroDashboardRequest(
        UUID buffetId,
        LocalDate date,
        LocalDate from,
        LocalDate to
) {
}
