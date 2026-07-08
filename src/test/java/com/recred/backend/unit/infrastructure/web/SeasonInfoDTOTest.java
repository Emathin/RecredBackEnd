package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.SeasonInfoResponse;
import com.recred.backend.infrastructure.web.SeasonInfoDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SeasonInfoDTOTest {

    @Test
    void constructorAndAccessorsShouldExposeValues() {
        SeasonInfoDTO dto = new SeasonInfoDTO("Invierno", 20, "Primavera");

        assertEquals("Invierno", dto.getCurrentSeason());
        assertEquals(20, dto.getDaysRemaining());
        assertEquals("Primavera", dto.getNextSeason());

        dto.setCurrentSeason("Verano");
        dto.setDaysRemaining(45);
        dto.setNextSeason("Oto\u00f1o");

        assertEquals("Verano", dto.getCurrentSeason());
        assertEquals(45, dto.getDaysRemaining());
        assertEquals("Oto\u00f1o", dto.getNextSeason());
    }

    @Test
    void fromApplicationShouldMapResponseAndAcceptNull() {
        SeasonInfoDTO dto = SeasonInfoDTO.fromApplication(
                new SeasonInfoResponse("Primavera", 5, "Verano")
        );

        assertEquals("Primavera", dto.getCurrentSeason());
        assertEquals(5, dto.getDaysRemaining());
        assertEquals("Verano", dto.getNextSeason());
        assertNull(SeasonInfoDTO.fromApplication(null));
    }
}
