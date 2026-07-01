package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ListKiosqueroOrdersRequest(
        UUID buffetId,
        List<EstadoCompra> statuses,
        LocalDate date,
        UUID pickupSlotId,
        Integer limit
) {
}
