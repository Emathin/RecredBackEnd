package com.recred.backend.application;

import com.recred.backend.domain.models.DailyCloseStatus;
import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class GetDailyBuffetCloseStatus {

    private final DailyBuffetCloseRepository dailyBuffetCloseRepository;

    public GetDailyBuffetCloseStatus(
            DailyBuffetCloseRepository dailyBuffetCloseRepository
    ) {
        this.dailyBuffetCloseRepository = dailyBuffetCloseRepository;
    }

    public DailyCloseStatus execute(UUID buffetId, LocalDate date) {
        UUID requiredBuffetId = required(buffetId, "buffetId is required");
        LocalDate requiredDate = required(date, "date is required");

        return dailyBuffetCloseRepository.findByBuffetAndDate(requiredBuffetId, requiredDate)
                .map(DailyCloseStatus::closed)
                .orElseGet(() -> DailyCloseStatus.open(requiredBuffetId, requiredDate));
    }

    private <T> T required(T value, String message) {
        return Optional.ofNullable(value)
                .orElseThrow(() -> new IllegalArgumentException(message));
    }
}
