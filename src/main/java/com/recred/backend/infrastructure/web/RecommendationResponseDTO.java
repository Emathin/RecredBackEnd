package com.recred.backend.infrastructure.web;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RecommendationResponseDTO {
    @JsonProperty("sugerencias")
    private List<SuggestionDTO> sugerences;
    @JsonProperty("tip_promocional")
    private String promocionalTip;

    public RecommendationResponseDTO() {
    }

    public List<SuggestionDTO> getSugerences() {
        return sugerences;
    }

    public void setSugerences(List<SuggestionDTO> sugerences) {
        this.sugerences = sugerences;
    }

    public String getPromocionalTip() {
        return promocionalTip;
    }

    public void setPromocionalTip(String promocionalTip) {
        this.promocionalTip = promocionalTip;
    }
}
