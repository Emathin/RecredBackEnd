package com.recred.backend.infrastructure.web;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RecommendationResponseDTO {
    @JsonProperty("sugerencias")
    private List<SugerenciaDTO> sugerences;
    @JsonProperty("tip_promocional")
    private String promocionalTip;

    public RecommendationResponseDTO() {
    }

    public List<SugerenciaDTO> getSugerences() {
        return sugerences;
    }

    public void setSugerences(List<SugerenciaDTO> sugerences) {
        this.sugerences = sugerences;
    }

    public String getPromocionalTip() {
        return promocionalTip;
    }

    public void setPromocionalTip(String promocionalTip) {
        this.promocionalTip = promocionalTip;
    }
}
