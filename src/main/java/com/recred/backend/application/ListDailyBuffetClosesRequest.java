package com.recred.backend.application;

import java.time.LocalDate;
import java.util.UUID;

public record ListDailyBuffetClosesRequest(
        UUID buffetId,
        LocalDate startDate,
        LocalDate endDate
) {
}
