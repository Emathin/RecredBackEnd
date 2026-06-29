package com.recred.backend.application;

import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;

import java.util.Optional;

public class GetDailyBuffetCloseStatus {

    private final DailyBuffetCloseRepository dailyBuffetCloseRepository;

    public GetDailyBuffetCloseStatus(
            DailyBuffetCloseRepository dailyBuffetCloseRepository
    ) {
        this.dailyBuffetCloseRepository = dailyBuffetCloseRepository;
    }

    public GetDailyBuffetCloseStatusResponse execute(
            GetDailyBuffetCloseStatusRequest request
    ) {
        validate(request);
        return dailyBuffetCloseRepository.findByBuffetAndDate(
                        request.buffetId(),
                        request.date())
                .map(GetDailyBuffetCloseStatusResponse::closed)
                .orElseGet(() -> GetDailyBuffetCloseStatusResponse.open(
                        request.buffetId(),
                        request.date()));
    }

    private void validate(GetDailyBuffetCloseStatusRequest request) {
        required(request, "request is required");
        required(request.buffetId(), "buffetId is required");
        required(request.date(), "date is required");
    }

    private <T> T required(T value, String message) {
        return Optional.ofNullable(value)
                .orElseThrow(() -> new IllegalArgumentException(message));
    }
}
