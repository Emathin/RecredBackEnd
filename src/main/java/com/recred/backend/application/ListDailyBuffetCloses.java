package com.recred.backend.application;

import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ListDailyBuffetCloses {

    private final DailyBuffetCloseRepository dailyBuffetCloseRepository;

    public ListDailyBuffetCloses(
            DailyBuffetCloseRepository dailyBuffetCloseRepository
    ) {
        this.dailyBuffetCloseRepository = dailyBuffetCloseRepository;
    }

    public List<DailyBuffetClose> execute(
            UUID buffetId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        UUID requiredBuffetId = required(buffetId, "buffetId is required");
        validateRange(startDate, endDate);

        return dailyBuffetCloseRepository.findByBuffetAndDateRange(
                requiredBuffetId,
                startDate,
                endDate
        );
    }

    private <T> T required(T value, String message) {
        return Optional.ofNullable(value)
                .orElseThrow(() -> new IllegalArgumentException(message));
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        Optional.ofNullable(startDate)
                .filter(start -> endDate != null && start.isAfter(endDate))
                .map(start -> invalidRangeException())
                .ifPresent(ListDailyBuffetCloses::throwException);
    }

    private IllegalArgumentException invalidRangeException() {
        return new IllegalArgumentException("startDate must be before or equal to endDate");
    }

    private static void throwException(RuntimeException exception) {
        throw exception;
    }
}
