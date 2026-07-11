package com.recred.backend.domain.models;

public enum RecessTime {
    FIRST_RECESS("primer recreo"),
    SECOND_RECESS("segundo recreo"),
    NOON("mediodia"),
    AFTER_HOURS("salida"),
    ELEVEN_AM("11:00 hs"),
    FOUR_PM("16:00 hs");

    private final String label;

    RecessTime(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
