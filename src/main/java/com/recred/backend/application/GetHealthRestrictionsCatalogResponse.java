package com.recred.backend.application;

import java.util.List;

public record GetHealthRestrictionsCatalogResponse(
        List<GetHealthRestrictionsResponse> restrictions
) {
}
