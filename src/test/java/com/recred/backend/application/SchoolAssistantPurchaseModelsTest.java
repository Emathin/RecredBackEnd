package com.recred.backend.application;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SchoolAssistantPurchaseModelsTest {

    private static final UUID DUMMY_ID = UUID.randomUUID();

    @Test
    void shouldAccumulateLatestDateWhenBothDatesExist() {
        LocalDateTime olderDate = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime newerDate = LocalDateTime.of(2026, 6, 2, 10, 0);
        PurchasePatternAccumulator older = givenAccumulatorWithDate(olderDate);
        PurchasePatternAccumulator newer = givenAccumulatorWithDate(newerDate);

        PurchasePattern pattern = whenMerging(older, newer);
        PurchasePattern reversePattern = whenMerging(newer, older);

        thenOccurrencesAre(2, pattern);
        thenLastDateIs(newerDate, pattern);
        thenLastDateIs(newerDate, reversePattern);
    }

    @Test
    void shouldReturnNullLatestDateWhenAnyDateIsMissing() {
        LocalDateTime someDate = LocalDateTime.of(2026, 6, 1, 10, 0);
        PurchasePatternAccumulator withDate = givenAccumulatorWithDate(someDate);
        PurchasePatternAccumulator withoutDate = givenAccumulatorWithoutDate();

        PurchasePattern pattern = whenMerging(withDate, withoutDate);

        thenOccurrencesAre(2, pattern);
        thenItemsArePresent(pattern);
        thenLastDateIsNull(pattern);
    }

    private PurchasePatternAccumulator givenAccumulatorWithDate(LocalDateTime date) {
        return new PurchasePatternAccumulator(
                new PurchasePatternCandidate("key", List.of(dummyItem()), date)
        );
    }

    private PurchasePatternAccumulator givenAccumulatorWithoutDate() {
        return new PurchasePatternAccumulator(
                new PurchasePatternCandidate("key", List.of(dummyItem()), null)
        );
    }

    private PurchaseItemRequest dummyItem() {
        return new PurchaseItemRequest(DUMMY_ID, 1, DUMMY_ID);
    }

    private PurchasePattern whenMerging(PurchasePatternAccumulator first, PurchasePatternAccumulator second) {
        return first.merge(second).pattern();
    }

    private void thenOccurrencesAre(int expectedOccurrences, PurchasePattern pattern) {
        assertEquals(expectedOccurrences, pattern.occurrences());
    }

    private void thenLastDateIs(LocalDateTime expectedDate, PurchasePattern pattern) {
        assertEquals(expectedDate, pattern.lastDate());
    }

    private void thenLastDateIsNull(PurchasePattern pattern) {
        assertNull(pattern.lastDate());
    }

    private void thenItemsArePresent(PurchasePattern pattern) {
        assertEquals(List.of(dummyItem()), pattern.items());
    }
}

