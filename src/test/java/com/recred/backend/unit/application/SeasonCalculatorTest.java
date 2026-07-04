package com.recred.backend.unit.application;

import com.recred.backend.application.SeasonCalculator;
import com.recred.backend.application.SeasonInfoResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeasonCalculatorTest {

    @Test
    void calculateCurrentSeasonShouldReturnSummerBeforeAutumn() {
        SeasonInfoResponse response = SeasonCalculator.calculateCurrentSeason(
                LocalDate.of(2026, 1, 10)
        );

        assertEquals("Verano", response.currentSeason());
        assertEquals("Oto\u00f1o", response.nextSeason());
        assertEquals(70, response.daysRemaining());
    }

    @Test
    void calculateCurrentSeasonShouldReturnAutumn() {
        SeasonInfoResponse response = SeasonCalculator.calculateCurrentSeason(
                LocalDate.of(2026, 4, 1)
        );

        assertEquals("Oto\u00f1o", response.currentSeason());
        assertEquals("Invierno", response.nextSeason());
        assertEquals(81, response.daysRemaining());
    }

    @Test
    void calculateCurrentSeasonShouldReturnWinter() {
        SeasonInfoResponse response = SeasonCalculator.calculateCurrentSeason(
                LocalDate.of(2026, 7, 1)
        );

        assertEquals("Invierno", response.currentSeason());
        assertEquals("Primavera", response.nextSeason());
        assertEquals(82, response.daysRemaining());
    }

    @Test
    void calculateCurrentSeasonShouldReturnSpring() {
        SeasonInfoResponse response = SeasonCalculator.calculateCurrentSeason(
                LocalDate.of(2026, 10, 1)
        );

        assertEquals("Primavera", response.currentSeason());
        assertEquals("Verano", response.nextSeason());
        assertEquals(81, response.daysRemaining());
    }

    @Test
    void calculateCurrentSeasonShouldReturnSummerAcrossYearEnd() {
        SeasonInfoResponse response = SeasonCalculator.calculateCurrentSeason(
                LocalDate.of(2026, 12, 22)
        );

        assertEquals("Verano", response.currentSeason());
        assertEquals("Oto\u00f1o", response.nextSeason());
        assertEquals(89, response.daysRemaining());
    }
}
