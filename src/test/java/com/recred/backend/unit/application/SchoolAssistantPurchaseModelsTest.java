package com.recred.backend.application;


import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SchoolAssistantPurchaseModelsTest {

    @Test
    void purchasePatternAccumulatorUsesLatestDateWhenBothDatesExist() {
        PurchaseItemRequest item = item();
        LocalDateTime olderDate = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime newerDate = LocalDateTime.of(2026, 6, 2, 10, 0);
        PurchasePatternAccumulator older = new PurchasePatternAccumulator(
                new PurchasePatternCandidate("key", List.of(item), olderDate)
        );
        PurchasePatternAccumulator newer = new PurchasePatternAccumulator(
                new PurchasePatternCandidate("key", List.of(item), newerDate)
        );

        PurchasePattern pattern = older.merge(newer).pattern();
        PurchasePattern reversePattern = newer.merge(older).pattern();

        assertEquals(2, pattern.occurrences());
        assertEquals(newerDate, pattern.lastDate());
        assertEquals(newerDate, reversePattern.lastDate());
    }

    @Test
    void purchasePatternAccumulatorUsesNullLatestDateWhenAnyDateIsMissing() {
        PurchaseItemRequest item = item();
        PurchasePatternAccumulator withDate = new PurchasePatternAccumulator(
                new PurchasePatternCandidate(
                        "key",
                        List.of(item),
                        LocalDateTime.of(2026, 6, 1, 10, 0)
                )
        );
        PurchasePatternAccumulator withoutDate = new PurchasePatternAccumulator(
                new PurchasePatternCandidate("key", List.of(item), null)
        );

        PurchasePattern pattern = withDate.merge(withoutDate).pattern();

        assertEquals(2, pattern.occurrences());
        assertEquals(List.of(item), pattern.items());
        assertNull(pattern.lastDate());
    }

    private PurchaseItemRequest item() {
        return new PurchaseItemRequest(UUID.randomUUID(), 1, UUID.randomUUID());
    }
}
