package com.recred.backend.application;

import java.time.LocalDate;
import java.util.UUID;

public record RegisterBuffetRequest(
        UUID schoolId,
        String name,
        LocalDate habilitationExpirationDate
) {
}
