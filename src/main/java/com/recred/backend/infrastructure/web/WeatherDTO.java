package com.recred.backend.infrastructure.web;

public class WeatherDTO {
    private Double temperature;
    private String condition;
    private String ciudad;
    private String provincia;

    public WeatherDTO(Double temperature, String condition) {
        this.temperature = temperature;
        this.condition = condition;
    }

    public WeatherDTO(Double temperature, String condition, String ciudad, String provincia) {
        this.temperature = temperature;
        this.condition = condition;
        this.ciudad = ciudad;
        this.provincia = provincia;
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

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }
}
