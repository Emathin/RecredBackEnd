package com.recred.backend.application;

import java.util.List;

public record GenerateBuffetMenuResponse(
        List<MenuProductResponse> products
) {
}
