package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.*;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FranjaHorariaTest {

    @Test
    void minutosCorteSeguroShouldReturnZeroForNullOrNegativeValues() {
        FranjaHoraria franja = new FranjaHoraria();

        assertEquals(0, franja.minutosCorteSeguro());

        franja.setMinutosCorte(-5);

        assertEquals(0, franja.minutosCorteSeguro());
    }

    @Test
    void estaDentroDeLaFranjaShouldReturnFalseWhenMomentIsNull() {
        FranjaHoraria franja = new FranjaHoraria(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Primer recreo",
                LocalTime.of(9, 0),
                LocalTime.of(9, 15),
                true
        );

        assertFalse(franja.estaDentroDeLaFranja(null));
        assertTrue(franja.estaDentroDeLaFranja(
                LocalDateTime.of(2026, 6, 16, 9, 10)
        ));
    }
}
