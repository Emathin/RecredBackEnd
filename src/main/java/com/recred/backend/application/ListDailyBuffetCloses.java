package com.recred.backend.application;

import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;
import java.util.List;

public class ListDailyBuffetCloses {

    private final DailyBuffetCloseRepository dailyBuffetCloseRepository;

    public ListDailyBuffetCloses(
            DailyBuffetCloseRepository dailyBuffetCloseRepository
    ) {
        this.dailyBuffetCloseRepository = dailyBuffetCloseRepository;
    }

    public ListDailyBuffetClosesResponse execute(ListDailyBuffetClosesRequest request) {
        return ListDailyBuffetClosesResponse.fromDomain(findClosesInRange(request));
    }

    private List<DailyBuffetClose> findClosesInRange(ListDailyBuffetClosesRequest request) {
        return dailyBuffetCloseRepository.findByBuffetAndDateRange(
                request.buffetId(),
                request.startDate(),
                request.endDate()
        );
    }
}
