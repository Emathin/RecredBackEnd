package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.IntervalUnit;
import com.recred.backend.domain.models.KiosqueroPayoutConfig;
import com.recred.backend.domain.models.PayoutScheduleStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KiosqueroPayoutConfigTest {

    @Test
    void validateIntervalConfig_WhenValidDays_ShouldNotThrowException() {
        KiosqueroPayoutConfig config = new KiosqueroPayoutConfig();
        config.setUnidadIntervalo(IntervalUnit.DAYS);
        config.setCantidadIntervalo(10);

        config.validateIntervalConfig();
    }

    @Test
    void validateIntervalConfig_WhenInvalidDays_ShouldThrowException() {
        KiosqueroPayoutConfig config = new KiosqueroPayoutConfig();
        config.setUnidadIntervalo(IntervalUnit.DAYS);
        config.setCantidadIntervalo(5);

        assertThrows(IllegalArgumentException.class, config::validateIntervalConfig);
    }

    @ParameterizedTest
    @CsvSource({
            "WEEKS, 1",
            "WEEKS, 2",
            "MONTHS, 1",
            "MONTHS, 3"
    })
    void validateIntervalConfig_WhenValidIntervals_ShouldNotThrowException(
            String unitStr, int amount) {
        KiosqueroPayoutConfig config = new KiosqueroPayoutConfig();
        config.setUnidadIntervalo(IntervalUnit.valueOf(unitStr));
        config.setCantidadIntervalo(amount);

        config.validateIntervalConfig();
    }

    @Test
    void validateIntervalConfig_WhenNullUnit_ShouldThrowException() {
        KiosqueroPayoutConfig config = new KiosqueroPayoutConfig();
        config.setCantidadIntervalo(1);

        assertThrows(IllegalArgumentException.class, config::validateIntervalConfig);
    }

    @Test
    void calculateNextPayout_ShouldSetNextPayoutDate() {
        KiosqueroPayoutConfig config = new KiosqueroPayoutConfig();
        config.setUnidadIntervalo(IntervalUnit.DAYS);
        config.setCantidadIntervalo(1);
        LocalDate baseDate = LocalDate.of(2026, 7, 2);

        config.calculateNextPayout(baseDate);

        assertEquals(LocalDate.of(2026, 7, 3), config.getProximaEjecucion());
    }

    @Test
    void isDue_WhenActiveAndDue_ShouldReturnTrue() {
        KiosqueroPayoutConfig config = new KiosqueroPayoutConfig();
        config.setEstado(PayoutScheduleStatus.ACTIVE);
        config.setProximaEjecucion(LocalDate.of(2026, 7, 1));

        boolean due = config.isDue(LocalDate.of(2026, 7, 2));

        assertTrue(due);
    }

    @Test
    void isDue_WhenNotActive_ShouldReturnFalse() {
        KiosqueroPayoutConfig config = new KiosqueroPayoutConfig();
        config.setEstado(PayoutScheduleStatus.PAUSED);
        config.setProximaEjecucion(LocalDate.of(2026, 7, 1));

        boolean due = config.isDue(LocalDate.of(2026, 7, 2));

        assertFalse(due);
    }

    @Test
    void isDue_WhenFutureDate_ShouldReturnFalse() {
        KiosqueroPayoutConfig config = new KiosqueroPayoutConfig();
        config.setEstado(PayoutScheduleStatus.ACTIVE);
        config.setProximaEjecucion(LocalDate.of(2026, 7, 5));

        boolean due = config.isDue(LocalDate.of(2026, 7, 2));

        assertFalse(due);
    }
}
