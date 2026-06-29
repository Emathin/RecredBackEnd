package com.recred.backend.domain.models;

import java.util.List;
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
    private static final Set<EstadoCompra> STATISTICAL_CONSUMPTION_ALERT_STATUSES =
            Set.of(ENTREGADO);
    private static final Set<EstadoCompra> STUDENT_CONSUMPTION_STATUSES = Set.of(ENTREGADO);
    private static final List<EstadoCompra> ACTIVE_TRACKING_STATUSES = List.of(
            PENDIENTE,
            EN_PREPARACION,
            LISTO
    );

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

    public static Set<EstadoCompra> statisticalConsumptionAlertStatuses() {
        return STATISTICAL_CONSUMPTION_ALERT_STATUSES;
    }

    public static Set<EstadoCompra> studentConsumptionStatuses() {
        return STUDENT_CONSUMPTION_STATUSES;
    }

    public static List<EstadoCompra> activeTrackingStatuses() {
        return ACTIVE_TRACKING_STATUSES;
    }
}
