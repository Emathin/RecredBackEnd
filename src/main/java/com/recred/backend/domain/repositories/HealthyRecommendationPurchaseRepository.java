package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.HealthyProductOption;
import com.recred.backend.domain.models.RecentProductConsumption;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface HealthyRecommendationPurchaseRepository {

    List<RecentProductConsumption> findConsumptions(
            UUID studentId,
            Collection<EstadoCompra> statuses,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    List<HealthyProductOption> findAvailableOptions(
            UUID studentId,
            UUID buffetId,
            UUID excludedProductId,
            Collection<EstadoInventario> states
    );
}
