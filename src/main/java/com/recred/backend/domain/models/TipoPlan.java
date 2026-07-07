package com.recred.backend.domain.models;

public enum TipoPlan {
    GRATUITO,
    INTERMEDIO,
    AVANZADO;

    public boolean isFree() {
        return this == GRATUITO;
    }

    public boolean isPaid() {
        return !isFree();
    }

    public boolean atLeast(TipoPlan minimumPlan) {
        if (minimumPlan == null) {
            throw new IllegalArgumentException("Minimum plan is required");
        }

        return level() >= minimumPlan.level();
    }

    private int level() {
        return switch (this) {
            case GRATUITO -> 0;
            case INTERMEDIO -> 1;
            case AVANZADO -> 2;
        };
    }
}
