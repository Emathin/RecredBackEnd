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
        
        String currentSeason;
        String nextSeason;
        LocalDate nextSeasonDate;
        int currentYear = date.getYear();

        if (isBetween(currentMonthDay, SUMMER_START, AUTUMN_START)) {
            currentSeason = "Verano";
            nextSeason = "Otoño";
            nextSeasonDate = AUTUMN_START.atYear(currentYear);
        } else if (isBetween(currentMonthDay, AUTUMN_START, WINTER_START)) {
            currentSeason = "Otoño";
            nextSeason = "Invierno";
            nextSeasonDate = WINTER_START.atYear(currentYear);
        } else if (isBetween(currentMonthDay, WINTER_START, SPRING_START)) {
            currentSeason = "Invierno";
            nextSeason = "Primavera";
            nextSeasonDate = SPRING_START.atYear(currentYear);
        } else {
            currentSeason = "Primavera";
            nextSeason = "Verano";
            if (currentMonthDay.isBefore(SUMMER_START)) {
                nextSeasonDate = SUMMER_START.atYear(currentYear);
            } else {
                nextSeasonDate = SUMMER_START.atYear(currentYear + 1);
            }
        }

        if (currentSeason.equals("Verano")) {
            int autumnYear = currentMonthDay.isBefore(AUTUMN_START) ? currentYear : currentYear + 1;
            nextSeasonDate = AUTUMN_START.atYear(autumnYear);
        }

        int daysRemaining = (int) ChronoUnit.DAYS.between(date, nextSeasonDate);

        return new SeasonInfoResponse(currentSeason, daysRemaining, nextSeason);
    }

    private static boolean isBetween(MonthDay target, MonthDay start, MonthDay end) {
        if (start.isAfter(end)) {
            return !target.isBefore(start) || target.isBefore(end);
        }
        return !target.isBefore(start) && target.isBefore(end);
    }
}
