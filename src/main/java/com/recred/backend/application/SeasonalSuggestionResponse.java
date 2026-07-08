package com.recred.backend.application;

import java.util.List;

public record SeasonalSuggestionResponse(
        String category,
        String action,
        String reason,
        String icon,
        List<MenuProductResponse> topProducts
) {
}
