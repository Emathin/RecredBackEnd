package com.recred.backend.domain.models;

import java.time.LocalDate;

public record SchoolCalendarEvent(
        LocalDate date,
        DayType type,
        String description
) {
}
