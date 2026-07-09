package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.TipoPlan;
import java.util.UUID;

public record UserSubscriptionTrialRequest(
        UUID usuarioId,
        TipoPlan plan
) {
}
