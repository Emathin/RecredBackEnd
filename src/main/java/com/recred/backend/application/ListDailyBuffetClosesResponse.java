package com.recred.backend.application;

import com.recred.backend.domain.models.DailyBuffetClose;
import java.util.List;

public record ListDailyBuffetClosesResponse(List<DailyBuffetCloseResponse> closes) {
    public static ListDailyBuffetClosesResponse fromDomain(List<DailyBuffetClose> closes) {
        return new ListDailyBuffetClosesResponse(closes.stream()
                .map(DailyBuffetCloseResponse::fromDomain)
                .toList());
    }
}
