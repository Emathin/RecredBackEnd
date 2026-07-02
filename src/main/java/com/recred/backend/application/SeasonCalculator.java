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
            // Si estamos antes de fin de año y todavía es primavera, el verano empieza este año
            if (currentMonthDay.isBefore(SUMMER_START)) {
                nextSeasonDate = SUMMER_START.atYear(currentYear);
            } else {
                nextSeasonDate = SUMMER_START.atYear(currentYear + 1);
            }
        }

        // Caso especial para Verano cruzando el año nuevo
        if (currentSeason.equals("Verano") && currentMonthDay.isBefore(AUTUMN_START)) {
            // El otoño es este año
            nextSeasonDate = AUTUMN_START.atYear(currentYear);
        }

        int daysRemaining = (int) ChronoUnit.DAYS.between(date, nextSeasonDate);

        return new SeasonInfoResponse(currentSeason, daysRemaining, nextSeason);
    }

    private static boolean isBetween(MonthDay target, MonthDay start, MonthDay end) {
        if (start.isAfter(end)) { // Wrap around the year
            return !target.isBefore(start) || target.isBefore(end);
        }
        return !target.isBefore(start) && target.isBefore(end);
    }
}
