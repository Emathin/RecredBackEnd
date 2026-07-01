package com.recred.backend.application;

import java.time.LocalDate;
import java.util.UUID;

public record GenerateDailyBuffetReportRequest(UUID buffetId, LocalDate date) {
}
