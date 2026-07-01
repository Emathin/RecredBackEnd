package com.recred.backend.application;

import java.util.Set;
import java.util.UUID;

public record GetCartSuggestionsRequest(
        UUID studentId,
        UUID buffetId,
        Set<UUID> cartProductIds,
        Integer limit
) {

    public void validate() {
        if (studentId == null) {
            throw new IllegalArgumentException("El alumno es obligatorio");
        }
        if (buffetId == null) {
            throw new IllegalArgumentException("El buffet es obligatorio");
        }
    }
}
