package com.recred.backend.application;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;

public class SeasonCalculator {

    private static final MonthDay SUMMER_START = MonthDay.of(12, 21);
    private static final MonthDay AUTUMN_START = MonthDay.of(3, 21);
    private static final MonthDay WINTER_START = MonthDay.of(6, 21);
    private static final MonthDay SPRING_START = MonthDay.of(9, 21);

    public static SeasonInfoResponse calculateCurrentSeason(LocalDate date) {
        MonthDay currentMonthDay = MonthDay.from(date);
        SeasonPeriod season = seasonFor(currentMonthDay);
        LocalDate nextSeasonDate = nextSeasonDate(date, season.nextStart());
        int daysRemaining = (int) ChronoUnit.DAYS.between(date, nextSeasonDate);

        return new SeasonInfoResponse(
                season.currentSeason(),
                daysRemaining,
                season.nextSeason()
        );
    }

    private static SeasonPeriod seasonFor(MonthDay currentMonthDay) {
        if (isBetween(currentMonthDay, SUMMER_START, AUTUMN_START)) {
            return new SeasonPeriod("Verano", "Otoño", AUTUMN_START);
        }
        if (isBetween(currentMonthDay, AUTUMN_START, WINTER_START)) {
            return new SeasonPeriod("Otoño", "Invierno", WINTER_START);
        }
        if (isBetween(currentMonthDay, WINTER_START, SPRING_START)) {
            return new SeasonPeriod("Invierno", "Primavera", SPRING_START);
        }
        return new SeasonPeriod("Primavera", "Verano", SUMMER_START);
    }

    private static LocalDate nextSeasonDate(LocalDate date, MonthDay nextStart) {
        LocalDate nextDate = nextStart.atYear(date.getYear());
        if (nextDate.isBefore(date) || nextDate.equals(date)) {
            return nextStart.atYear(date.getYear() + 1);
        }
        return nextDate;
    }

    private static boolean isBetween(MonthDay target, MonthDay start, MonthDay end) {
        if (start.isAfter(end)) {
            return !target.isBefore(start) || target.isBefore(end);
        }
        return !target.isBefore(start) && target.isBefore(end);
    }

    private record SeasonPeriod(
            String currentSeason,
            String nextSeason,
            MonthDay nextStart
    ) {
    }
}
