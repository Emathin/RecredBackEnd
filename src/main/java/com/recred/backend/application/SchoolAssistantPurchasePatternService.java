package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

class SchoolAssistantPurchasePatternService {

    private static final int PURCHASE_PATTERN_LOOKBACK = 30;
    private static final int MIN_FREQUENT_PURCHASE_OCCURRENCES = 2;

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final SchoolAssistantContentFormatter contentFormatter;

    SchoolAssistantPurchasePatternService(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            SchoolAssistantContentFormatter contentFormatter
    ) {
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.contentFormatter = contentFormatter;
    }

    Optional<PurchasePlan> lastRebuyablePlan(UUID studentId) {
        return purchaseHistory(studentId).stream()
                .filter(Purchase::isRebuyable)
                .findFirst()
                .flatMap(lastPurchase -> planFromPurchase(studentId, lastPurchase, 1));
    }

    Optional<PurchasePlan> mostFrequentPlan(UUID studentId) {
        return purchasePatterns(studentId).stream()
                .map(pattern -> planFromRequests(
                        studentId,
                        pattern.items(),
                        pattern.occurrences(),
                        pattern.lastDate()
                ))
                .flatMap(Optional::stream)
                .findFirst();
    }

    List<PurchasePattern> frequentPatterns(UUID studentId, int limit) {
        return purchasePatterns(studentId).stream()
                .limit(limit)
                .toList();
    }

    private Optional<PurchasePlan> planFromPurchase(
            UUID studentId,
            Purchase purchase,
            int occurrences
    ) {
        return planFromRequests(
                studentId,
                itemRequests(purchase),
                occurrences,
                purchase.getDate()
        );
    }

    private Optional<PurchasePlan> planFromRequests(
            UUID studentId,
            List<PurchaseItemRequest> requests,
            int occurrences,
            LocalDateTime lastDate
    ) {
        List<PurchasePlanItem> items = requests.stream()
                .map(this::planItem)
                .flatMap(Optional::stream)
                .toList();
        Optional<UUID> buffetId = buffetId(requests);

        return hasCompletePurchasePlan(items, requests, buffetId)
                ? Optional.of(new PurchasePlan(
                        studentId,
                        buffetId.orElseThrow(),
                        items,
                        occurrences,
                        lastDate
                ))
                : Optional.empty();
    }

    private boolean hasCompletePurchasePlan(
            List<PurchasePlanItem> items,
            List<PurchaseItemRequest> requests,
            Optional<UUID> buffetId
    ) {
        return items.size() == requests.size()
                && !items.isEmpty()
                && buffetId.isPresent();
    }

    private Optional<UUID> buffetId(List<PurchaseItemRequest> requests) {
        return requests.stream()
                .map(PurchaseItemRequest::buffetId)
                .filter(Objects::nonNull)
                .findFirst();
    }

    private Optional<PurchasePlanItem> planItem(PurchaseItemRequest request) {
        return productRepository.findActiveById(request.productId())
                .map(product -> new PurchasePlanItem(
                        product.getId(),
                        contentFormatter.productName(product),
                        request.quantity(),
                        contentFormatter.price(product)
                ));
    }

    private List<PurchasePattern> purchasePatterns(UUID studentId) {
        return purchaseHistory(studentId).stream()
                .filter(Purchase::isRebuyable)
                .limit(PURCHASE_PATTERN_LOOKBACK)
                .map(this::patternCandidate)
                .filter(candidate -> !candidate.items().isEmpty())
                .collect(Collectors.toMap(
                        PurchasePatternCandidate::key,
                        PurchasePatternAccumulator::new,
                        PurchasePatternAccumulator::merge,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .map(PurchasePatternAccumulator::pattern)
                .filter(this::hasMinimumOccurrences)
                .sorted(this::comparePatterns)
                .toList();
    }

    private PurchasePatternCandidate patternCandidate(Purchase purchase) {
        List<PurchaseItemRequest> items = itemRequests(purchase);
        String key = items.stream()
                .map(item -> item.productId() + ":" + item.quantity())
                .collect(Collectors.joining("|"));
        return new PurchasePatternCandidate(key, items, purchase.getDate());
    }

    private List<PurchaseItemRequest> itemRequests(Purchase purchase) {
        return purchase.getItems().entrySet()
                .stream()
                .filter(entry -> isValidPurchaseItem(entry.getKey(), entry.getValue()))
                .map(entry -> new PurchaseItemRequest(
                        entry.getKey().getId(),
                        entry.getValue(),
                        purchase.getBuffetId()
                ))
                .sorted(Comparator.comparing(item -> item.productId().toString()))
                .toList();
    }

    private List<Purchase> purchaseHistory(UUID studentId) {
        return purchaseRepository.findHistorialByAlumno(studentId);
    }

    private boolean hasMinimumOccurrences(PurchasePattern pattern) {
        return pattern.occurrences() >= MIN_FREQUENT_PURCHASE_OCCURRENCES;
    }

    private int comparePatterns(PurchasePattern first, PurchasePattern second) {
        int occurrencesComparison = Integer.compare(second.occurrences(), first.occurrences());
        return occurrencesComparison != 0
                ? occurrencesComparison
                : compareDates(first.lastDate(), second.lastDate());
    }

    private int compareDates(LocalDateTime first, LocalDateTime second) {
        return Comparator.nullsLast(LocalDateTime::compareTo)
                .reversed()
                .compare(first, second);
    }

    private boolean isValidPurchaseItem(Producto product, Integer quantity) {
        return product != null
                && product.getId() != null
                && quantity != null
                && quantity > 0;
    }
}
