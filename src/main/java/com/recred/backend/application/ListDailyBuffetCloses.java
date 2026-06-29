package com.recred.backend.application;

import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;

import java.time.LocalDate;
import java.util.Optional;

public class ListDailyBuffetCloses {

    private final DailyBuffetCloseRepository dailyBuffetCloseRepository;

    public ListDailyBuffetCloses(
            DailyBuffetCloseRepository dailyBuffetCloseRepository
    ) {
        this.dailyBuffetCloseRepository = dailyBuffetCloseRepository;
    }

    public ListDailyBuffetClosesResponse execute(ListDailyBuffetClosesRequest request) {
        validate(request);
        return ListDailyBuffetClosesResponse.fromDomain(
                dailyBuffetCloseRepository.findByBuffetAndDateRange(
                        request.buffetId(),
                        request.startDate(),
                        request.endDate()
                ));
    }

    private void validate(ListDailyBuffetClosesRequest request) {
        required(request, "request is required");
        required(request.buffetId(), "buffetId is required");
        validateRange(request.startDate(), request.endDate());
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
