package com.recred.backend.application;

import com.recred.backend.config.StatisticalConsumptionAlertProperties;
import com.recred.backend.domain.models.ConsumptionAlert;
import com.recred.backend.domain.models.ConsumptionAlertAnalysisRequest;
import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.ConsumptionAlertPattern;
import com.recred.backend.domain.models.ConsumptionAlertType;
import com.recred.backend.domain.models.ConsumptionCategoryStatistic;
import com.recred.backend.domain.models.ConsumptionProductStatistic;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.repositories.ConsumptionAlertNotificationRepository;
import com.recred.backend.domain.repositories.ConsumptionAlertPurchaseRepository;
import com.recred.backend.domain.repositories.ConsumptionAlertStudentRepository;
import com.recred.backend.domain.services.ConsumptionAlertAnalyzer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public class GenerateStatisticalConsumptionAlerts {

    private static final int MINIMUM_LOOKBACK_DAYS = 1;

    private final ConsumptionAlertStudentRepository studentRepository;
    private final ConsumptionAlertPurchaseRepository purchaseRepository;
    private final ConsumptionAlertNotificationRepository notificationRepository;
    private final ConsumptionAlertAnalyzer analyzer;
    private final StatisticalConsumptionAlertPatternCalculator patternCalculator;
    private final StatisticalConsumptionAlertProperties properties;
    private final Clock clock;

    public GenerateStatisticalConsumptionAlerts(
            ConsumptionAlertStudentRepository studentRepository,
            ConsumptionAlertPurchaseRepository purchaseRepository,
            ConsumptionAlertNotificationRepository notificationRepository,
            ConsumptionAlertAnalyzer analyzer,
            StatisticalConsumptionAlertProperties properties,
            Clock clock
    ) {
        this.studentRepository = studentRepository;
        this.purchaseRepository = purchaseRepository;
        this.notificationRepository = notificationRepository;
        this.analyzer = analyzer;
        this.patternCalculator = new StatisticalConsumptionAlertPatternCalculator();
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public GenerateStatisticalConsumptionAlertsResponse execute(
            GenerateStatisticalConsumptionAlertsRequest request
    ) {
        requireRequest(request);
        LocalDate calculationDate = LocalDate.now(clock);
        DateWindow dateWindow = dateWindow(calculationDate);
        return new GenerateStatisticalConsumptionAlertsResponse(
                generateAlerts(calculationDate, dateWindow)
        );
    }

    private void requireRequest(GenerateStatisticalConsumptionAlertsRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
    }

    private int generateAlerts(
            LocalDate calculationDate,
            DateWindow dateWindow
    ) {
        int generatedAlerts = 0;
        for (ConsumptionAlertCandidate candidate : studentRepository.findActiveCandidates()) {
            if (saveAlert(candidate, calculationDate, dateWindow)) {
                generatedAlerts++;
            }
        }
        return generatedAlerts;
    }

    private boolean saveAlert(
            ConsumptionAlertCandidate candidate,
            LocalDate calculationDate,
            DateWindow dateWindow
    ) {
        return alertFor(candidate, calculationDate, dateWindow)
                .map(this::saveAlert)
                .orElse(false);
    }

    private boolean saveAlert(ConsumptionAlert alert) {
        notificationRepository.save(alert);
        return true;
    }

    private Optional<ConsumptionAlert> alertFor(
            ConsumptionAlertCandidate candidate,
            LocalDate calculationDate,
            DateWindow dateWindow
    ) {
        if (shouldSkipForDailyLimit(candidate, dateWindow)) {
            return Optional.empty();
        }

        return buildAlert(candidate, calculationDate, dateWindow);
    }

    private Optional<ConsumptionAlert> buildAlert(
            ConsumptionAlertCandidate candidate,
            LocalDate calculationDate,
            DateWindow dateWindow
    ) {
        long purchaseCount = purchaseRepository.countPurchases(
                candidate.studentId(),
                EstadoCompra.statisticalConsumptionAlertStatuses(),
                dateWindow.startDateTime(),
                dateWindow.endDateTime()
        );
        ConsumptionAlertType alertType = alertType(purchaseCount);

        if (shouldSkipForCooldown(candidate, alertType, calculationDate)) {
            return Optional.empty();
        }

        ConsumptionAlertPattern pattern = buildPattern(
                candidate,
                alertType,
                calculationDate,
                dateWindow,
                purchaseCount
        );
        return analyzer.analyze(request(candidate, pattern))
                .map(messages -> new ConsumptionAlert(candidate, pattern, messages));
    }

    private boolean shouldSkipForDailyLimit(
            ConsumptionAlertCandidate candidate,
            DateWindow dateWindow
    ) {
        return notificationRepository.hasAnyAlertBetween(
                candidate.studentId(),
                dateWindow.todayStartDateTime(),
                dateWindow.endDateTime()
        );
    }

    private boolean shouldSkipForCooldown(
            ConsumptionAlertCandidate candidate,
            ConsumptionAlertType alertType,
            LocalDate calculationDate
    ) {
        LocalDateTime cooldownStartDateTime = calculationDate
                .minusDays(cooldownDays(alertType))
                .atStartOfDay();
        return notificationRepository.hasAlertSince(
                candidate.studentId(),
                alertType,
                cooldownStartDateTime
        );
    }

    private ConsumptionAlertPattern buildPattern(
            ConsumptionAlertCandidate candidate,
            ConsumptionAlertType alertType,
            LocalDate calculationDate,
            DateWindow dateWindow,
            long purchaseCount
    ) {
        return patternCalculator.calculate(
                candidate,
                alertType,
                calculationDate,
                dateWindow.alertDateWindow(),
                purchaseCount,
                categoryStatistics(candidate, alertType, dateWindow),
                suggestedProduct(candidate, alertType, dateWindow)
        );
    }

    private List<ConsumptionCategoryStatistic> categoryStatistics(
            ConsumptionAlertCandidate candidate,
            ConsumptionAlertType alertType,
            DateWindow dateWindow
    ) {
        if (alertType.isHealthyRecommendation()) {
            return List.of();
        }

        return purchaseRepository.findCategoryStatistics(
                candidate.studentId(),
                EstadoCompra.statisticalConsumptionAlertStatuses(),
                dateWindow.startDateTime(),
                dateWindow.endDateTime()
        );
    }

    private Optional<ConsumptionProductStatistic> suggestedProduct(
            ConsumptionAlertCandidate candidate,
            ConsumptionAlertType alertType,
            DateWindow dateWindow
    ) {
        if (alertType.isHealthyRecommendation()) {
            return Optional.empty();
        }

        return purchaseRepository.findProductStatistics(
                        candidate.studentId(),
                        EstadoCompra.statisticalConsumptionAlertStatuses(),
                        dateWindow.startDateTime(),
                        dateWindow.endDateTime()
                )
                .stream()
                .filter(this::isProductSuggestionStrongEnough)
                .max(Comparator.comparing(ConsumptionProductStatistic::quantity));
    }

    private boolean isProductSuggestionStrongEnough(ConsumptionProductStatistic statistic) {
        return statistic.hasQuantityAtLeast(properties.minimumProductQuantityForSuggestion());
    }

    private ConsumptionAlertAnalysisRequest request(
            ConsumptionAlertCandidate candidate,
            ConsumptionAlertPattern pattern
    ) {
        return new ConsumptionAlertAnalysisRequest(
                pattern.type(),
                candidate.studentName(),
                pattern.calculationDate(),
                pattern.startDate(),
                pattern.endDate(),
                pattern.purchaseCount(),
                pattern.totalAmount(),
                pattern.mainCategory(),
                pattern.mainCategoryPercentage(),
                pattern.suggestedProductName()
        );
    }

    private ConsumptionAlertType alertType(long purchaseCount) {
        return ConsumptionAlertType.fromPurchaseCount(
                purchaseCount,
                properties.minimumPurchasesForPattern()
        );
    }

    private int cooldownDays(ConsumptionAlertType alertType) {
        if (alertType.isStatisticalPattern()) {
            return properties.patternCooldownDays();
        }
        return properties.healthyRecommendationCooldownDays();
    }

    private DateWindow dateWindow(LocalDate calculationDate) {
        int lookbackDays = Math.max(properties.lookbackDays(), MINIMUM_LOOKBACK_DAYS);
        LocalDate startDate = calculationDate.minusDays(lookbackDays - 1L);
        return new DateWindow(
                startDate,
                calculationDate,
                startDate.atStartOfDay(),
                calculationDate.atTime(LocalTime.MAX),
                calculationDate.atStartOfDay()
        );
    }

    private record DateWindow(
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            LocalDateTime todayStartDateTime
    ) {

        private StatisticalConsumptionAlertPatternCalculator.AlertDateWindow alertDateWindow() {
            return new StatisticalConsumptionAlertPatternCalculator.AlertDateWindow(
                    startDate,
                    endDate
            );
        }
    }
}
