package com.recred.backend.domain.models;

import java.util.UUID;

public record ConsumptionAlertTutor(
        UUID userId,
        String name,
        TipoPlan plan
) {

    public boolean hasAdvancedPlan() {
        return plan != null && plan.atLeast(TipoPlan.AVANZADO);
    }
}
