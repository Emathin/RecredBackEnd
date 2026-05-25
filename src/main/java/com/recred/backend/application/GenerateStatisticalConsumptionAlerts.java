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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenerateStatisticalConsumptionAlerts {

    private static final int MINIMUM_LOOKBACK_DAYS = 1;
    private static final int PERCENTAGE_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");
    private static final Set<EstadoCompra> PURCHASE_STATUSES = Set.of(EstadoCompra.ENTREGADO);

    private final ConsumptionAlertStudentRepository studentRepository;
    private final ConsumptionAlertPurchaseRepository purchaseRepository;
    private final ConsumptionAlertNotificationRepository notificationRepository;
    private final ConsumptionAlertAnalyzer analyzer;
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
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public int execute() {
        LocalDate calculationDate = LocalDate.now(clock);
        DateWindow dateWindow = dateWindow(calculationDate);

        int generatedAlerts = 0;
        for (ConsumptionAlertCandidate candidate : studentRepository.findActiveCandidates()) {
            if (shouldSkipForDailyLimit(candidate, dateWindow)) {
                continue;
            }

            Optional<ConsumptionAlert> alert = buildAlert(candidate, calculationDate, dateWindow);
            if (alert.isEmpty()) {
                continue;
            }

            notificationRepository.save(alert.orElseThrow());
            generatedAlerts++;
        }
        return generatedAlerts;
    }

    private Optional<ConsumptionAlert> buildAlert(
            ConsumptionAlertCandidate candidate,
            LocalDate calculationDate,
            DateWindow dateWindow
    ) {
        long purchaseCount = purchaseRepository.countPurchases(
                candidate.studentId(),
                PURCHASE_STATUSES,
                dateWindow.startDateTime(),
                dateWindow.endDateTime()
        );
        ConsumptionAlertType alertType = resolveAlertType(purchaseCount);

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
        if (alertType == ConsumptionAlertType.HEALTHY_RECOMMENDATION) {
            return healthyPattern(candidate, calculationDate, dateWindow, purchaseCount);
        }

        List<ConsumptionCategoryStatistic> categoryStatistics =
                purchaseRepository.findCategoryStatistics(
                        candidate.studentId(),
                        PURCHASE_STATUSES,
                        dateWindow.startDateTime(),
                        dateWindow.endDateTime()
                );
        ConsumptionCategoryStatistic mainCategory = categoryStatistics.stream()
                .max(Comparator.comparing(statistic -> amount(statistic.totalAmount())))
                .orElse(null);
        BigDecimal totalAmount = totalAmount(categoryStatistics);
        ConsumptionProductStatistic suggestedProduct = suggestedProduct(candidate, dateWindow);

        return new ConsumptionAlertPattern(
                alertType,
                candidate.studentId(),
                calculationDate,
                dateWindow.startDate(),
                dateWindow.endDate(),
                purchaseCount,
                totalAmount,
                mainCategory == null ? null : mainCategory.category(),
                categoryPercentage(mainCategory, totalAmount),
                suggestedProduct == null ? null : suggestedProduct.productId(),
                suggestedProduct == null ? null : suggestedProduct.productName()
        );
    }

    private ConsumptionAlertPattern healthyPattern(
            ConsumptionAlertCandidate candidate,
            LocalDate calculationDate,
            DateWindow dateWindow,
            long purchaseCount
    ) {
        return new ConsumptionAlertPattern(
                ConsumptionAlertType.HEALTHY_RECOMMENDATION,
                candidate.studentId(),
                calculationDate,
                dateWindow.startDate(),
                dateWindow.endDate(),
                purchaseCount,
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                null,
                null
        );
    }

    private ConsumptionProductStatistic suggestedProduct(
            ConsumptionAlertCandidate candidate,
            DateWindow dateWindow
    ) {
        return purchaseRepository.findProductStatistics(
                        candidate.studentId(),
                        PURCHASE_STATUSES,
                        dateWindow.startDateTime(),
                        dateWindow.endDateTime()
                )
                .stream()
                .filter(this::isProductSuggestionStrongEnough)
                .max(Comparator.comparing(ConsumptionProductStatistic::quantity))
                .orElse(null);
    }

    private boolean isProductSuggestionStrongEnough(ConsumptionProductStatistic statistic) {
        return statistic.quantity() != null
                && statistic.quantity() >= properties.minimumProductQuantityForSuggestion();
    }

    private BigDecimal totalAmount(List<ConsumptionCategoryStatistic> statistics) {
        return statistics.stream()
                .map(ConsumptionCategoryStatistic::totalAmount)
                .map(this::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal amount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount;
    }

    private BigDecimal categoryPercentage(
            ConsumptionCategoryStatistic mainCategory,
            BigDecimal totalAmount
    ) {
        if (mainCategory == null || totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return mainCategory.totalAmount()
                .multiply(ONE_HUNDRED)
                .divide(totalAmount, PERCENTAGE_SCALE, RoundingMode.HALF_UP);
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

    private ConsumptionAlertType resolveAlertType(long purchaseCount) {
        if (purchaseCount >= properties.minimumPurchasesForPattern()) {
            return ConsumptionAlertType.STATISTICAL_PATTERN;
        }
        return ConsumptionAlertType.HEALTHY_RECOMMENDATION;
    }

    private int cooldownDays(ConsumptionAlertType alertType) {
        if (alertType == ConsumptionAlertType.STATISTICAL_PATTERN) {
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
    }
}
