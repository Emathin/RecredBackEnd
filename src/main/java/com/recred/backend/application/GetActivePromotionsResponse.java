package com.recred.backend.application;

import java.util.List;

public record GetActivePromotionsResponse(List<CreatePromotionResponse> promotions) {
}
