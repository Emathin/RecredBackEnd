package com.recred.backend.domain.models;

public enum RewardLevel {
    BRONZE(10, "¡Buen comienzo! Seguí eligiendo sano."),
    SILVER(25, "¡Excelente ritmo! Tu cuerpo te lo agradece."),
    GOLD(50, "¡Sos un experto saludable! Recompensa máxima alcanzada.");

    private final int points;
    private final String message;

    RewardLevel(int points, String message) {
        this.points = points;
        this.message = message;
    }

    public int getPoints() {
        return points;
    }

    public String getMessage() {
        return message;
    }
}
