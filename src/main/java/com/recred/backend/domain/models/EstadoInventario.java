package com.recred.backend.domain.models;

import java.util.Set;

public enum EstadoInventario {
    DISPONIBLE,
    BAJO_STOCK,
    SIN_STOCK,
    DESACTIVADO;

    private static final Set<EstadoInventario> HEALTHY_RECOMMENDATION_AVAILABLE_STATES =
            Set.of(DISPONIBLE, BAJO_STOCK);

    public static Set<EstadoInventario> availableForHealthyRecommendations() {
        return HEALTHY_RECOMMENDATION_AVAILABLE_STATES;
    }
}
