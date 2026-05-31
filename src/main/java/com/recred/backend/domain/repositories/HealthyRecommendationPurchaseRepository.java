package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.PurchaseState;
import com.recred.backend.domain.models.InventoryState;
import com.recred.backend.domain.models.HealthyProductOption;
import com.recred.backend.domain.models.RecentProductConsumption;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface HealthyRecommendationPurchaseRepository {

    List<RecentProductConsumption> findConsumptions(
            UUID studentId,
            Collection<PurchaseState> statuses,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    List<HealthyProductOption> findAvailableOptions(
            UUID studentId,
            UUID buffetId,
            UUID excludedProductId,
            Collection<InventoryState> states
    );
}
