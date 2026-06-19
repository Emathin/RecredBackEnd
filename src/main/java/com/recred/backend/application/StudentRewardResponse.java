package com.recred.backend.application;

public record StudentRewardResponse(
        Integer totalPoints,
        String currentLevel,
        String levelMessage,
        Integer pointsToNextLevel,
        String nextLevelName
) {}
