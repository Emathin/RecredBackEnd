package com.recred.backend.infrastructure.web;

public class ClimaDTO {
    private Double temperatura;
    private String condicion;

    public ClimaDTO(Double temperatura, String condicion) {
        this.temperatura = temperatura;
        this.condicion = condicion;
    }

    public Double getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(Double temperatura) {
        this.temperatura = temperatura;
    }

    public String getCondicion() {
        return condicion;
    }

    public void setCondicion(String condicion) {
        this.condicion = condicion;
    }
}
