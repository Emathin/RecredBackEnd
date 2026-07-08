package com.recred.backend.application;

import com.recred.backend.domain.models.RecessTime;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

record PurchaseItemRequest(
        UUID productId,
        int quantity,
        UUID buffetId) {
}

record PurchasePlanItem(
        UUID productId,
        String productName,
        int quantity,
        BigDecimal unitPrice) {
    BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}

record PurchasePlan(
        UUID studentId,
        UUID buffetId,
        List<PurchasePlanItem> items,
        int occurrences,
        LocalDateTime lastDate) {
    BigDecimal totalAmount() {
        return items.stream()
                .map(PurchasePlanItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

record PurchasePatternCandidate(
        String key,
        List<PurchaseItemRequest> items,
        LocalDateTime lastDate) {
}

record PurchasePattern(
        List<PurchaseItemRequest> items,
        int occurrences,
        LocalDateTime lastDate) {
}

record PendingPurchase(
        UUID studentId,
        UUID buffetId,
        List<PurchaseItemRequest> items,
        Optional<LocalDate> pickupDate,
        Optional<RecessTime> recessTime) {
}

record PendingActionSearch(
        Optional<PendingPurchase> pendingPurchase,
        boolean shouldStop) {
    static PendingActionSearch terminal() {
        return new PendingActionSearch(Optional.empty(), true);
    }

    static PendingActionSearch from(Optional<PendingPurchase> pendingPurchase) {
        return new PendingActionSearch(pendingPurchase, pendingPurchase.isPresent());
    }
}

final class PurchasePatternAccumulator {

    private final List<PurchaseItemRequest> items;
    private final int occurrences;
    private final LocalDateTime lastDate;

    PurchasePatternAccumulator(PurchasePatternCandidate candidate) {
        this(candidate.items(), 1, candidate.lastDate());
    }

    private PurchasePatternAccumulator(
            List<PurchaseItemRequest> items,
            int occurrences,
            LocalDateTime lastDate) {
        this.items = items;
        this.occurrences = occurrences;
        this.lastDate = lastDate;
    }

    PurchasePatternAccumulator merge(PurchasePatternAccumulator other) {
        return new PurchasePatternAccumulator(
                items,
                occurrences + other.occurrences,
                latestDate(lastDate, other.lastDate));
    }

    PurchasePattern pattern() {
        return new PurchasePattern(items, occurrences, lastDate);
    }

    private LocalDateTime latestDate(LocalDateTime first, LocalDateTime second) {
        if (first == null || second == null) {
            return null;
        }

        return first.compareTo(second) >= 0 ? first : second;
    }
}
