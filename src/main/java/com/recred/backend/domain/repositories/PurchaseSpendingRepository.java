package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.PurchaseState;
import com.recred.backend.domain.models.ConsumptionCategory;
import com.recred.backend.domain.models.PurchaseSpending;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PurchaseSpendingRepository {

    List<PurchaseSpending> findSpending(
            UUID studentId,
            Collection<PurchaseState> statuses,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    List<ConsumptionCategory> findMostConsumedCategories(
            UUID studentId,
            Collection<PurchaseState> statuses,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );
}
