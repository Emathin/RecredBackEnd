package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.SeasonInfoResponse;
import com.recred.backend.infrastructure.web.SeasonInfoDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SeasonInfoDTOTest {

    @Test
    void givenValuesWhenCreatingDtoThenAccessorsExposeValues() {
        SeasonInfoDTO dto = new SeasonInfoDTO("Invierno", 20, "Primavera");

        thenDtoHasValues(dto, "Invierno", 20, "Primavera");

        whenUpdatingDtoValues(dto, "Verano", 45, "Oto\u00f1o");

        thenDtoHasValues(dto, "Verano", 45, "Oto\u00f1o");
    }

    @Test
    void givenApplicationResponseWhenMappingToDtoThenMapsValues() {
        SeasonInfoDTO dto = whenMappingFromApplication(new SeasonInfoResponse("Primavera", 5, "Verano"));

        thenDtoHasValues(dto, "Primavera", 5, "Verano");
    }

    @Test
    void givenNullResponseWhenMappingToDtoThenReturnsNull() {
        SeasonInfoDTO dto = whenMappingFromApplication(null);

        thenDtoIsNull(dto);
    }

    private void whenUpdatingDtoValues(SeasonInfoDTO dto, String current, int days, String next) {
        dto.setCurrentSeason(current);
        dto.setDaysRemaining(days);
        dto.setNextSeason(next);
    }

    private SeasonInfoDTO whenMappingFromApplication(SeasonInfoResponse response) {
        return SeasonInfoDTO.fromApplication(response);
    }

    private void thenDtoHasValues(SeasonInfoDTO dto, String current, int days, String next) {
        assertEquals(current, dto.getCurrentSeason());
        assertEquals(days, dto.getDaysRemaining());
        assertEquals(next, dto.getNextSeason());
    }

    private void thenDtoIsNull(SeasonInfoDTO dto) {
        assertNull(dto);
    }
}
