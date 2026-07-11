package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.WeatherResponse;
import com.recred.backend.infrastructure.web.WeatherDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WeatherDTOTest {

    private WeatherDTO dto;

    @Test
    void givenWeatherDTOWhenUsingConstructorThenExposesInitialValues() {
        dto = givenWeatherDTO(21.5, "Soleado");

        thenTemperatureIs(21.5);
        thenConditionIs("Soleado");
    }

    @Test
    void givenWeatherDTOWhenSettingValuesThenUpdatesValues() {
        dto = givenWeatherDTO(0.0, "None");

        whenSettingValues(18.0, "Nublado", "Rosario", "Santa Fe");

        thenTemperatureIs(18.0);
        thenConditionIs("Nublado");
        thenCiudadIs("Rosario");
        thenProvinciaIs("Santa Fe");
    }

    @Test
    void givenApplicationModelWhenMappingFromApplicationThenReturnsMappedDTO() {
        WeatherResponse response = givenWeatherResponse(
                17.2, "Lluvia", "Cordoba", "Cordoba");

        dto = whenMappingFromApplication(response);

        thenTemperatureIs(17.2);
        thenConditionIs("Lluvia");
        thenCiudadIs("Cordoba");
        thenProvinciaIs("Cordoba");
    }

    @Test
    void givenNullApplicationModelWhenMappingFromApplicationThenReturnsNull() {
        dto = whenMappingFromApplication(null);

        assertNull(dto);
    }

    private WeatherDTO givenWeatherDTO(double temperature, String condition) {
        return new WeatherDTO(temperature, condition);
    }

    private WeatherResponse givenWeatherResponse(
            double temperature, String condition,
            String ciudad, String provincia) {
        return new WeatherResponse(temperature, condition, ciudad, provincia);
    }

    private void whenSettingValues(
            double temperature, String condition,
            String ciudad, String provincia) {
        dto.setTemperature(temperature);
        dto.setCondition(condition);
        dto.setCiudad(ciudad);
        dto.setProvincia(provincia);
    }

    private WeatherDTO whenMappingFromApplication(WeatherResponse response) {
        return WeatherDTO.fromApplication(response);
    }

    private void thenTemperatureIs(double expected) {
        assertEquals(expected, dto.getTemperature());
    }

    private void thenConditionIs(String expected) {
        assertEquals(expected, dto.getCondition());
    }

    private void thenCiudadIs(String expected) {
        assertEquals(expected, dto.getCiudad());
    }

    private void thenProvinciaIs(String expected) {
        assertEquals(expected, dto.getProvincia());
    }
}
