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
    void calculateCurrentSeasonShouldReturnAutumnWinterAndSpring() {
        SeasonInfoResponse autumn = SeasonCalculator.calculateCurrentSeason(
                LocalDate.of(2026, 4, 1)
        );
        SeasonInfoResponse winter = SeasonCalculator.calculateCurrentSeason(
                LocalDate.of(2026, 7, 1)
        );
        SeasonInfoResponse spring = SeasonCalculator.calculateCurrentSeason(
                LocalDate.of(2026, 10, 1)
        );

        assertEquals("Oto\u00f1o", autumn.currentSeason());
        assertEquals("Invierno", autumn.nextSeason());
        assertEquals(81, autumn.daysRemaining());
        assertEquals("Invierno", winter.currentSeason());
        assertEquals("Primavera", winter.nextSeason());
        assertEquals(82, winter.daysRemaining());
        assertEquals("Primavera", spring.currentSeason());
        assertEquals("Verano", spring.nextSeason());
        assertEquals(81, spring.daysRemaining());
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
