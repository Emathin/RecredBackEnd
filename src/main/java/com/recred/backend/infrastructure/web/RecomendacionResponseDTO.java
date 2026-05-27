package com.recred.backend.infrastructure.web;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RecomendacionResponseDTO {
    private List<SugerenciaDTO> sugerencias;
    @JsonProperty("tip_promocional")
    private String tipPromocional;

    public RecomendacionResponseDTO() {
    }

    public List<SugerenciaDTO> getSugerencias() {
        return sugerencias;
    }

    public void setSugerencias(List<SugerenciaDTO> sugerencias) {
        this.sugerencias = sugerencias;
    }

    public String getTipPromocional() {
        return tipPromocional;
    }

    public void setTipPromocional(String tipPromocional) {
        this.tipPromocional = tipPromocional;
    }
}
