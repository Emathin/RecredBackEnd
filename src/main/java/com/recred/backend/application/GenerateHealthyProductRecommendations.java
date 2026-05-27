package com.recred.backend.application;

import com.recred.backend.config.HealthyRecommendationProperties;
import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.HealthyProductOption;
import com.recred.backend.domain.models.HealthyProductRecommendation;
import com.recred.backend.domain.models.HealthyRecommendationAnalysisRequest;
import com.recred.backend.domain.models.HealthyRecommendationMessages;
import com.recred.backend.domain.models.HealthyRecommendationPattern;
import com.recred.backend.domain.models.RecentProductConsumption;
import com.recred.backend.domain.repositories.ConsumptionAlertStudentRepository;
import com.recred.backend.domain.repositories.HealthyRecommendationNotificationRepository;
import com.recred.backend.domain.repositories.HealthyRecommendationPurchaseRepository;
import com.recred.backend.domain.services.HealthyRecommendationAnalyzer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

public class GenerateHealthyProductRecommendations {

    private static final int MINIMUM_LOOKBACK_DAYS = 1;
    private static final int MINIMUM_REPEATED_DAYS = 1;
    private static final Set<EstadoCompra> PURCHASE_STATUSES = Set.of(EstadoCompra.ENTREGADO);
    private static final Set<EstadoInventario> AVAILABLE_STATES = Set.of(
            EstadoInventario.DISPONIBLE,
            EstadoInventario.BAJO_STOCK
    );

    private final ConsumptionAlertStudentRepository studentRepository;
    private final HealthyRecommendationPurchaseRepository purchaseRepository;
    private final HealthyRecommendationNotificationRepository notificationRepository;
    private final HealthyRecommendationAnalyzer analyzer;
    private final HealthyRecommendationProperties properties;
    private final Clock clock;

    public GenerateHealthyProductRecommendations(
            ConsumptionAlertStudentRepository studentRepository,
            HealthyRecommendationPurchaseRepository purchaseRepository,
            HealthyRecommendationNotificationRepository notificationRepository,
            HealthyRecommendationAnalyzer analyzer,
            HealthyRecommendationProperties properties,
            Clock clock
    ) {
        this.studentRepository = studentRepository;
        this.purchaseRepository = purchaseRepository;
        this.notificationRepository = notificationRepository;
        this.analyzer = analyzer;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public int execute() {
        LocalDate calculationDate = LocalDate.now(clock);
        DateWindow dateWindow = dateWindow(calculationDate);

        int generatedRecommendations = 0;
        for (ConsumptionAlertCandidate candidate : studentRepository.findActiveCandidates()) {
            if (shouldSkipForCooldown(candidate, calculationDate)) {
                continue;
            }

            Optional<HealthyProductRecommendation> recommendation = buildRecommendation(
                    candidate,
                    calculationDate,
                    dateWindow
            );
            if (recommendation.isEmpty()) {
                continue;
            }

            notificationRepository.save(recommendation.orElseThrow());
            generatedRecommendations++;
        }
        return generatedRecommendations;
    }

    private boolean shouldSkipForCooldown(
            ConsumptionAlertCandidate candidate,
            LocalDate calculationDate
    ) {
        LocalDateTime cooldownStart = calculationDate.minusDays(properties.cooldownDays())
                .atStartOfDay();
        return notificationRepository.hasRecommendationSince(candidate.studentId(), cooldownStart);
    }

    private Optional<HealthyProductRecommendation> buildRecommendation(
            ConsumptionAlertCandidate candidate,
            LocalDate calculationDate,
            DateWindow dateWindow
    ) {
        List<RecentProductConsumption> consumptions = purchaseRepository.findConsumptions(
                candidate.studentId(),
                PURCHASE_STATUSES,
                dateWindow.startDateTime(),
                dateWindow.endDateTime()
        );

        return repeatedPatterns(consumptions).stream()
                .map(pattern -> buildRecommendationWithAiSuggestion(
                        candidate,
                        calculationDate,
                        dateWindow,
                        consumptions,
                        pattern
                ))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private List<RepeatedProductPattern> repeatedPatterns(
            List<RecentProductConsumption> consumptions
    ) {
        int minimumRepeatedDays = Math.max(
                properties.minimumRepeatedDays(),
                MINIMUM_REPEATED_DAYS
        );
        Map<ProductPatternKey, List<RecentProductConsumption>> consumptionsByProduct =
                consumptions.stream().collect(Collectors.groupingBy(ProductPatternKey::from));

        return consumptionsByProduct.values()
                .stream()
                .map(this::toPattern)
                .filter(pattern -> pattern.repeatedDays() >= minimumRepeatedDays)
                .sorted(patternComparator())
                .toList();
    }

    private RepeatedProductPattern toPattern(List<RecentProductConsumption> consumptions) {
        RecentProductConsumption firstConsumption = consumptions.get(0);
        int repeatedDays = consumptions.stream()
                .map(consumption -> consumption.purchaseDateTime().toLocalDate())
                .collect(Collectors.toSet())
                .size();
        long totalQuantity = consumptions.stream()
                .map(RecentProductConsumption::quantity)
                .mapToLong(quantity -> quantity == null ? 0 : quantity)
                .sum();
        LocalDateTime latestPurchaseDateTime = consumptions.stream()
                .map(RecentProductConsumption::purchaseDateTime)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.MIN);

        return new RepeatedProductPattern(
                firstConsumption.productId(),
                firstConsumption.productName(),
                firstConsumption.categoryName(),
                firstConsumption.buffetId(),
                repeatedDays,
                totalQuantity,
                latestPurchaseDateTime
        );
    }

    private Comparator<RepeatedProductPattern> patternComparator() {
        return Comparator.comparing(RepeatedProductPattern::repeatedDays)
                .thenComparing(RepeatedProductPattern::totalQuantity)
                .thenComparing(RepeatedProductPattern::latestPurchaseDateTime)
                .reversed();
    }

    private Optional<HealthyProductRecommendation> buildRecommendationWithAiSuggestion(
            ConsumptionAlertCandidate candidate,
            LocalDate calculationDate,
            DateWindow dateWindow,
            List<RecentProductConsumption> consumptions,
            RepeatedProductPattern pattern
    ) {
        List<HealthyProductOption> availableOptions = purchaseRepository.findAvailableOptions(
                        candidate.studentId(),
                        pattern.buffetId(),
                        pattern.productId(),
                        AVAILABLE_STATES
                )
                .stream()
                .limit(maxAvailableOptions())
                .toList();

        if (availableOptions.isEmpty()) {
            return Optional.empty();
        }

        return analyzer.analyze(request(
                        candidate,
                        calculationDate,
                        pattern,
                        consumptions,
                        availableOptions
                ))
                .flatMap(messages -> selectedOption(messages, availableOptions)
                        .map(option -> new HealthyProductRecommendation(
                                candidate,
                                pattern(candidate, calculationDate, dateWindow, pattern, option),
                                messages
                        )));
    }

    private HealthyRecommendationAnalysisRequest request(
            ConsumptionAlertCandidate candidate,
            LocalDate calculationDate,
            RepeatedProductPattern pattern,
            List<RecentProductConsumption> consumptions,
            List<HealthyProductOption> availableOptions
    ) {
        return new HealthyRecommendationAnalysisRequest(
                candidate.studentName(),
                calculationDate,
                consumptionsStartDate(consumptions),
                consumptionsEndDate(consumptions),
                pattern.productName(),
                pattern.categoryName(),
                pattern.repeatedDays(),
                pattern.totalQuantity(),
                limitedHistory(consumptions),
                availableOptions
        );
    }

    private LocalDate consumptionsStartDate(List<RecentProductConsumption> consumptions) {
        return consumptions.stream()
                .map(consumption -> consumption.purchaseDateTime().toLocalDate())
                .min(Comparator.naturalOrder())
                .orElse(LocalDate.now(clock));
    }

    private LocalDate consumptionsEndDate(List<RecentProductConsumption> consumptions) {
        return consumptions.stream()
                .map(consumption -> consumption.purchaseDateTime().toLocalDate())
                .max(Comparator.naturalOrder())
                .orElse(LocalDate.now(clock));
    }

    private List<RecentProductConsumption> limitedHistory(
            List<RecentProductConsumption> consumptions
    ) {
        return consumptions.stream()
                .sorted(Comparator.comparing(RecentProductConsumption::purchaseDateTime)
                        .reversed())
                .limit(maxHistoryItems())
                .toList();
    }

    private int maxHistoryItems() {
        return Math.max(properties.maxHistoryItems(), 1);
    }

    private int maxAvailableOptions() {
        return Math.max(properties.maxAvailableOptions(), 1);
    }

    private Optional<HealthyProductOption> selectedOption(
            HealthyRecommendationMessages messages,
            List<HealthyProductOption> availableOptions
    ) {
        return availableOptions.stream()
                .filter(option -> option.productId().equals(messages.suggestedProductId()))
                .findFirst();
    }

    private HealthyRecommendationPattern pattern(
            ConsumptionAlertCandidate candidate,
            LocalDate calculationDate,
            DateWindow dateWindow,
            RepeatedProductPattern repeatedPattern,
            HealthyProductOption option
    ) {
        return new HealthyRecommendationPattern(
                candidate.studentId(),
                calculationDate,
                dateWindow.startDate(),
                dateWindow.endDate(),
                repeatedPattern.productId(),
                repeatedPattern.productName(),
                repeatedPattern.categoryName(),
                repeatedPattern.buffetId(),
                repeatedPattern.repeatedDays(),
                repeatedPattern.totalQuantity(),
                option.productId(),
                option.productName(),
                option.categoryName()
        );
    }

    private DateWindow dateWindow(LocalDate calculationDate) {
        int lookbackDays = Math.max(properties.lookbackDays(), MINIMUM_LOOKBACK_DAYS);
        LocalDate endDate = calculationDate.minusDays(1);
        LocalDate startDate = endDate.minusDays(lookbackDays - 1L);
        return new DateWindow(
                startDate,
                endDate,
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX)
        );
    }

    private record ProductPatternKey(UUID productId, UUID buffetId) {

        private static ProductPatternKey from(RecentProductConsumption consumption) {
            return new ProductPatternKey(consumption.productId(), consumption.buffetId());
        }
    }

    private record RepeatedProductPattern(
            UUID productId,
            String productName,
            String categoryName,
            UUID buffetId,
            int repeatedDays,
            long totalQuantity,
            LocalDateTime latestPurchaseDateTime
    ) {
    }

    private record DateWindow(
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
    }
}
