package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.application.SeasonInfoResponse;

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

    public static SeasonInfoDTO fromApplication(SeasonInfoResponse response) {
        if (response == null) {
            return null;
        }

        return new SeasonInfoDTO(
                response.currentSeason(),
                response.daysRemaining(),
                response.nextSeason()
        );
    }
}
