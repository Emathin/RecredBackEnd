package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Purchase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseRepository {

    Purchase save(Purchase purchase);

    Optional<Purchase> findById(UUID id);

    BigDecimal sumExpensesByStudentInRange(UUID studentID,
                                           LocalDateTime start, LocalDateTime end);

    BigDecimal sumExpensesByCategoryInRange(UUID studentID,
                                            UUID categoryID, LocalDateTime start,
                                            LocalDateTime end);

    List<PreferenceAIRepository.PurchaseSummary> findLastPurchasesByStudent(
            UUID alumnoId, int limit);
}
