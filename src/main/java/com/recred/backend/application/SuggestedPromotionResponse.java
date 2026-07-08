package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.List;

public record SuggestedPromotionResponse(
        String name,
        BigDecimal discountPercentage,
        List<String> targetCategories,
        String image,
        List<String> productIds
) {
}
