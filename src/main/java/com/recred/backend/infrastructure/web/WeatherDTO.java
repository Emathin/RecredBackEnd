package com.recred.backend.infrastructure.web;

public class WeatherDTO {
    private Double temperature;
    private String condition;

    public WeatherDTO(Double temperature, String condition) {
        this.temperature = temperature;
        this.condition = condition;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}
