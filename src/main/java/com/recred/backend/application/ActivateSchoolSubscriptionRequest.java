package com.recred.backend.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ActivateSchoolSubscriptionRequest(
        UUID colegioId,
        BigDecimal precio,
        String moneda,
        LocalDateTime fechaInicio
) {
}
