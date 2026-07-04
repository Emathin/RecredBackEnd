package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.WeatherResponse;
import com.recred.backend.infrastructure.web.WeatherDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WeatherDTOTest {

    @Test
    void constructorsAndAccessorsShouldExposeWeatherValues() {
        WeatherDTO simple = new WeatherDTO(21.5, "Soleado");
        assertEquals(21.5, simple.getTemperature());
        assertEquals("Soleado", simple.getCondition());

        simple.setTemperature(18.0);
        simple.setCondition("Nublado");
        simple.setCiudad("Rosario");
        simple.setProvincia("Santa Fe");

        assertEquals(18.0, simple.getTemperature());
        assertEquals("Nublado", simple.getCondition());
        assertEquals("Rosario", simple.getCiudad());
        assertEquals("Santa Fe", simple.getProvincia());
    }

    @Test
    void fromApplicationShouldMapResponseAndAcceptNull() {
        WeatherDTO dto = WeatherDTO.fromApplication(new WeatherResponse(
                17.2,
                "Lluvia",
                "Cordoba",
                "Cordoba"
        ));

        assertEquals(17.2, dto.getTemperature());
        assertEquals("Lluvia", dto.getCondition());
        assertEquals("Cordoba", dto.getCiudad());
        assertEquals("Cordoba", dto.getProvincia());
        assertNull(WeatherDTO.fromApplication(null));
    }
}
