package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SeasonInfoDTO {
    
    @JsonProperty("estacion_actual")
    private String currentSeason;
    
    @JsonProperty("dias_restantes")
    private Integer daysRemaining;
    
    @JsonProperty("proxima_estacion")
    private String nextSeason;

    public SeasonInfoDTO() {}

    public SeasonInfoDTO(String currentSeason, Integer daysRemaining, String nextSeason) {
        this.currentSeason = currentSeason;
        this.daysRemaining = daysRemaining;
        this.nextSeason = nextSeason;
    }

    public String getCurrentSeason() {
        return currentSeason;
    }

    public void setCurrentSeason(String currentSeason) {
        this.currentSeason = currentSeason;
    }

    public Integer getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(Integer daysRemaining) {
        this.daysRemaining = daysRemaining;
    }

    public String getNextSeason() {
        return nextSeason;
    }

    public void setNextSeason(String nextSeason) {
        this.nextSeason = nextSeason;
    }
}
