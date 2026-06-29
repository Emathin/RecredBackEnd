package com.recred.backend.domain.models;

import java.util.Set;

public enum EstadoCompra {
    PENDIENTE("A preparar"),
    EN_PREPARACION("En preparacion"),
    LISTO("Listo para retirar"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado"),
    RECHAZADO("Rechazado"),
    VENCIDO("Vencido"),
    NO_RETIRADO("No retirado");

    private static final Set<EstadoCompra> HEALTHY_RECOMMENDATION_STATUSES = Set.of(ENTREGADO);

    private final String label;

    EstadoCompra(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Set<EstadoCompra> healthyRecommendationStatuses() {
        return HEALTHY_RECOMMENDATION_STATUSES;
    }
}
