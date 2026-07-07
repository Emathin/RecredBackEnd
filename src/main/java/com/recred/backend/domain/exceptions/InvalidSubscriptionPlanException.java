package com.recred.backend.domain.exceptions;

import com.recred.backend.domain.models.TipoPlan;

public class InvalidSubscriptionPlanException extends RuntimeException {

    public InvalidSubscriptionPlanException(TipoPlan plan) {
        super("Invalid subscription plan: " + plan);
    }
}
