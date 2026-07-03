package com.recred.backend.application;

public record SeasonInfoResponse(
        String currentSeason,
        Integer daysRemaining,
        String nextSeason
) {
}
