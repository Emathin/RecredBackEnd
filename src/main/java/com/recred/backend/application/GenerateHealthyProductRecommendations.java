package com.recred.backend.application;

import com.recred.backend.config.HealthyRecommendationProperties;
import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.HealthyProductOption;
import com.recred.backend.domain.models.HealthyProductRecommendation;
import com.recred.backend.domain.models.HealthyRepeatedProductPattern;
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
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public class GenerateHealthyProductRecommendations {

    private static final int MINIMUM_LOOKBACK_DAYS = 1;

    private final ConsumptionAlertStudentRepository studentRepository;
    private final HealthyRecommendationPurchaseRepository purchaseRepository;
    private final HealthyRecommendationNotificationRepository notificationRepository;
    private final HealthyRecommendationAnalyzer analyzer;
    private final HealthyRecommendationPatternCalculator patternCalculator;
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
        this.patternCalculator = new HealthyRecommendationPatternCalculator();
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public GenerateHealthyProductRecommendationsResponse execute(
            GenerateHealthyProductRecommendationsRequest request
    ) {
        requireRequest(request);
        LocalDate calculationDate = LocalDate.now(clock);
        DateWindow dateWindow = dateWindow(calculationDate);
        return new GenerateHealthyProductRecommendationsResponse(
                generateRecommendations(calculationDate, dateWindow)
        );
    }

    private void requireRequest(GenerateHealthyProductRecommendationsRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
    }

    private int generateRecommendations(
            LocalDate calculationDate,
            DateWindow dateWindow
    ) {
        int generatedRecommendations = 0;
        for (ConsumptionAlertCandidate candidate : studentRepository.findActiveCandidates()) {
            if (saveRecommendation(candidate, calculationDate, dateWindow)) {
                generatedRecommendations++;
            }
        }
        return generatedRecommendations;
    }

    private boolean saveRecommendation(
            ConsumptionAlertCandidate candidate,
            LocalDate calculationDate,
            DateWindow dateWindow
    ) {
        return recommendationFor(candidate, calculationDate, dateWindow)
                .map(this::saveRecommendation)
                .orElse(false);
    }

    private boolean saveRecommendation(HealthyProductRecommendation recommendation) {
        notificationRepository.save(recommendation);
        return true;
    }

    private Optional<HealthyProductRecommendation> recommendationFor(
            ConsumptionAlertCandidate candidate,
            LocalDate calculationDate,
            DateWindow dateWindow
    ) {
        if (shouldSkipForCooldown(candidate, calculationDate)) {
            return Optional.empty();
        }

        return buildRecommendation(candidate, calculationDate, dateWindow);
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
                EstadoCompra.healthyRecommendationStatuses(),
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

    private List<HealthyRepeatedProductPattern> repeatedPatterns(
            List<RecentProductConsumption> consumptions
    ) {
        return patternCalculator.calculate(consumptions, properties.minimumRepeatedDays());
    }

    private Optional<HealthyProductRecommendation> buildRecommendationWithAiSuggestion(
            ConsumptionAlertCandidate candidate,
            LocalDate calculationDate,
            DateWindow dateWindow,
            List<RecentProductConsumption> consumptions,
            HealthyRepeatedProductPattern pattern
    ) {
        List<HealthyProductOption> availableOptions = purchaseRepository.findAvailableOptions(
                        candidate.studentId(),
                        pattern.buffetId(),
                        pattern.productId(),
                        EstadoInventario.availableForHealthyRecommendations()
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
            HealthyRepeatedProductPattern pattern,
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
                .map(RecentProductConsumption::purchaseDate)
                .min(Comparator.naturalOrder())
                .orElse(LocalDate.now(clock));
    }

    private LocalDate consumptionsEndDate(List<RecentProductConsumption> consumptions) {
        return consumptions.stream()
                .map(RecentProductConsumption::purchaseDate)
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
                .filter(option -> option.isSuggestedBy(messages))
                .findFirst();
    }

    private HealthyRecommendationPattern pattern(
            ConsumptionAlertCandidate candidate,
            LocalDate calculationDate,
            DateWindow dateWindow,
            HealthyRepeatedProductPattern repeatedPattern,
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

    private record DateWindow(
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
    }
}
