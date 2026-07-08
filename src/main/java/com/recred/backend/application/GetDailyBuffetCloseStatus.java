package com.recred.backend.application;

import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;

public class GetDailyBuffetCloseStatus {

    private final DailyBuffetCloseRepository dailyBuffetCloseRepository;

    public GetDailyBuffetCloseStatus(
            DailyBuffetCloseRepository dailyBuffetCloseRepository
    ) {
        this.dailyBuffetCloseRepository = dailyBuffetCloseRepository;
    }

    public GetDailyBuffetCloseStatusResponse execute(
            GetDailyBuffetCloseStatusRequest statusRequest
    ) {
        return dailyBuffetCloseRepository.findByBuffetAndDate(
                        statusRequest.buffetId(),
                        statusRequest.date())
                .map(GetDailyBuffetCloseStatusResponse::closed)
                .orElseGet(() -> GetDailyBuffetCloseStatusResponse.open(
                        statusRequest.buffetId(),
                        statusRequest.date()));
    }
}
