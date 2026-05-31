package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ConsumptionCategoryStatistic;
import com.recred.backend.domain.models.ConsumptionProductStatistic;
import com.recred.backend.domain.models.PurchaseState;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ConsumptionAlertPurchaseRepository {

    long countPurchases(
            UUID studentId,
            Collection<PurchaseState> statuses,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    List<ConsumptionCategoryStatistic> findCategoryStatistics(
            UUID studentId,
            Collection<PurchaseState> statuses,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    List<ConsumptionProductStatistic> findProductStatistics(
            UUID studentId,
            Collection<PurchaseState> statuses,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );
}
