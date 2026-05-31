package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpenMeteoResponseDTO {
    private Current current;

    public Current getCurrent() {
        return current;
    }

    public void setCurrent(Current current) {
        this.current = current;
    }

    public static class Current {
        @JsonProperty("temperature_2m")
        private Double temperature;

        @JsonProperty("weather_code")
        private Integer weatherCode;

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Integer getWeatherCode() {
            return weatherCode;
        }

        public void setWeatherCode(Integer weatherCode) {
            this.weatherCode = weatherCode;
        }
    }
}
