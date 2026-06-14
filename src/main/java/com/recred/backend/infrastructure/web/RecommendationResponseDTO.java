package com.recred.backend.infrastructure.web;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RecommendationResponseDTO {
    @JsonProperty("sugerencias")
    private List<SugerenciaDTO> sugerences;
    @JsonProperty("tip_promocional")
    private String promocionalTip;
    
    @JsonProperty("promocion_sugerida")
    private SuggestedPromotionDTO suggestedPromotion;
    
    @JsonProperty("promocion_creada")
    private PromotionResponseDTO createdPromotion;

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

    public SuggestedPromotionDTO getSuggestedPromotion() {
        return suggestedPromotion;
    }

    public void setSuggestedPromotion(SuggestedPromotionDTO suggestedPromotion) {
        this.suggestedPromotion = suggestedPromotion;
    }

    public PromotionResponseDTO getCreatedPromotion() {
        return createdPromotion;
    }

    public void setCreatedPromotion(PromotionResponseDTO createdPromotion) {
        this.createdPromotion = createdPromotion;
    }
}
