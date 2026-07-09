package com.recred.backend.unit.application;

import com.recred.backend.application.SeasonCalculator;
import com.recred.backend.application.SeasonInfoResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeasonCalculatorTest {

    private SeasonInfoResponse response;
    private LocalDate date;

    @Test
    void calculateCurrentSeason_WhenDateIsBeforeAutumn_ReturnsSummer() {
        givenDate(2026, 1, 10);

        whenCalculatingCurrentSeason();

        thenSeasonInfoIs("Verano", "Otoño", 70);
    }

    @Test
    void calculateCurrentSeason_WhenDateIsInAutumn_ReturnsAutumn() {
        givenDate(2026, 4, 1);

        whenCalculatingCurrentSeason();

        thenSeasonInfoIs("Otoño", "Invierno", 81);
    }

    @Test
    void calculateCurrentSeason_WhenDateIsInWinter_ReturnsWinter() {
        givenDate(2026, 7, 1);

        whenCalculatingCurrentSeason();

        thenSeasonInfoIs("Invierno", "Primavera", 82);
    }

    @Test
    void calculateCurrentSeason_WhenDateIsInSpring_ReturnsSpring() {
        givenDate(2026, 10, 1);

        whenCalculatingCurrentSeason();

        thenSeasonInfoIs("Primavera", "Verano", 81);
    }

    @Test
    void calculateCurrentSeason_WhenDateIsAfterSummerStart_ReturnsSummerAcrossYearEnd() {
        givenDate(2026, 12, 22);

        whenCalculatingCurrentSeason();

        thenSeasonInfoIs("Verano", "Otoño", 89);
    }

    private void givenDate(int year, int month, int day) {
        date = LocalDate.of(year, month, day);
    }

    private void whenCalculatingCurrentSeason() {
        response = SeasonCalculator.calculateCurrentSeason(date);
    }

    private void thenSeasonInfoIs(
            String expectedCurrentSeason,
            String expectedNextSeason,
            int expectedDaysRemaining
    ) {
        assertEquals(expectedCurrentSeason, response.currentSeason());
        assertEquals(expectedNextSeason, response.nextSeason());
        assertEquals(expectedDaysRemaining, response.daysRemaining());
    }
}
