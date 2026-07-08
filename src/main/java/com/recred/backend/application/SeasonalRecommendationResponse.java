package com.recred.backend.application;

import java.util.List;

public record SeasonalRecommendationResponse(
        List<SeasonalSuggestionResponse> suggestions,
        String promotionalTip,
        SuggestedPromotionResponse suggestedPromotion,
        SeasonalCreatedPromotionResponse createdPromotion,
        SeasonInfoResponse seasonInfo,
        WeatherResponse currentWeather
) {
}
