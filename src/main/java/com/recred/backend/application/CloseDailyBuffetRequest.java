package com.recred.backend.application;

import java.time.LocalDate;
import java.util.UUID;

public record CloseDailyBuffetRequest(UUID buffetId, LocalDate date) {
}
