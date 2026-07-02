package com.recred.backend.infrastructure.web;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.application.SeasonalRecommendationResponse;

public class RecommendationResponseDTO {
    @JsonProperty("sugerencias")
    private List<SugerenciaDTO> sugerences;
    @JsonProperty("tip_promocional")
    private String promocionalTip;
    
    @JsonProperty("promocion_sugerida")
    private SuggestedPromotionDTO suggestedPromotion;
    
    @JsonProperty("promocion_creada")
    private PromotionResponseDTO createdPromotion;
    
    @JsonProperty("info_estacion")
    private SeasonInfoDTO seasonInfo;

    @JsonProperty("clima_actual")
    private WeatherDTO currentWeather;

    public RecommendationResponseDTO() {}

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

    public SeasonInfoDTO getSeasonInfo() {
        return seasonInfo;
    }

    public void setSeasonInfo(SeasonInfoDTO seasonInfo) {
        this.seasonInfo = seasonInfo;
    }

    public WeatherDTO getCurrentWeather() {
        return currentWeather;
    }

    public void setCurrentWeather(WeatherDTO currentWeather) {
        this.currentWeather = currentWeather;
    }

    public static RecommendationResponseDTO fromApplication(
            SeasonalRecommendationResponse response
    ) {
        RecommendationResponseDTO dto = new RecommendationResponseDTO();
        dto.setSugerences(response.suggestions() == null ? null : response.suggestions().stream()
                .map(SugerenciaDTO::fromApplication)
                .toList());
        dto.setPromocionalTip(response.promotionalTip());
        dto.setSuggestedPromotion(SuggestedPromotionDTO.fromApplication(
                response.suggestedPromotion()
        ));
        dto.setCreatedPromotion(PromotionResponseDTO.fromApplication(
                response.createdPromotion()
        ));
        dto.setSeasonInfo(SeasonInfoDTO.fromApplication(response.seasonInfo()));
        dto.setCurrentWeather(WeatherDTO.fromApplication(response.currentWeather()));
        return dto;
    }
}
