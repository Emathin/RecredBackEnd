package com.recred.backend.application;
import com.recred.backend.domain.models.EstadoCompra;
import java.time.LocalDateTime;
import java.util.UUID;
public record UpdatePurchaseStatusResponse(
        UUID id,
        UUID studentId,
        EstadoCompra status,
        String label,
        LocalDateTime date
) {}
