package com.recred.backend.domain.models;

public enum RewardLevel {
    PRINCIPIANTE(10, "¡Buen inicio! Seguí eligiendo sano para sumar puntos."),
    CRACK(25, "¡Estás a otro nivel! Ser sano te sale natural."),
    GOAT(50, "¡Sos el GOAT! Nadie te gana eligiendo snacks saludables.");

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

    public static RewardLevel forHealthyPurchaseHistory(long last7Days, long last15Days) {
        if (last15Days > 5) {
            return GOAT;
        }
        if (last7Days >= 3) {
            return CRACK;
        }
        return PRINCIPIANTE;
    }
}
