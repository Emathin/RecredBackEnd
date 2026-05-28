package com.recred.backend.domain.services;

import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.ConsumptionCategory;
import com.recred.backend.domain.models.PurchaseSpending;
import com.recred.backend.domain.models.SpendingPrediction;
import com.recred.backend.domain.models.SpendingPeriodPreference;
import com.recred.backend.domain.models.DateRange;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SpendingPredictionCalculator {

    private static final int AMOUNT_SCALE = 2;
    private static final int CONFIDENCE_SCALE = 4;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal NO_DATA_CONFIDENCE = BigDecimal.valueOf(0.10);
    private static final BigDecimal BASE_CONFIDENCE = BigDecimal.valueOf(0.35);
    private static final BigDecimal MAX_CONFIDENCE = BigDecimal.valueOf(0.95);

    public SpendingPrediction calculate(
            UUID studentId,
            BudgetPeriod period,
            LocalDate calculationDate,
            DateRange currentRange,
            DateRange historicalRange,
            List<PurchaseSpending> currentPeriodSpending,
            List<PurchaseSpending> historicalSpending,
            Optional<SpendingPeriodPreference> preference,
            List<ConsumptionCategory> mostConsumedCategories
    ) {
        BigDecimal currentSpending = sum(currentPeriodSpending);
        int remainingDays = countOperatingDays(calculationDate.plusDays(1), currentRange.endDate());
        int historicalDays = countOperatingDays(
                historicalRange.startDate(),
                historicalRange.endDate()
        );

        BigDecimal dailyAverage = calculateDailyAverage(
                historicalSpending,
                historicalDays,
                currentSpending,
                countOperatingDays(currentRange.startDate(), calculationDate)
        );
        BigDecimal predictedSpending = currentSpending.add(dailyAverage.multiply(
                BigDecimal.valueOf(remainingDays)));
        BigDecimal limitAmount = preference.map(SpendingPeriodPreference::limitAmount).orElse(null);
        BigDecimal budgetPercentage = calculatePercentage(predictedSpending, limitAmount);

        return new SpendingPrediction(
                studentId,
                period,
                calculationDate,
                currentRange.startDate(),
                currentRange.endDate(),
                scaleAmount(currentSpending),
                scaleAmount(predictedSpending),
                scaleAmount(dailyAverage),
                scaleAmount(limitAmount),
                budgetPercentage,
                calculateConfidence(historicalSpending, currentPeriodSpending),
                historicalDays,
                remainingDays,
                mostConsumedCategories
        );
    }

    public int countOperatingDays(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            return 0;
        }

        int days = 0;
        LocalDate date = from;
        while (!date.isAfter(to)) {
            if (isOperatingDay(date)) {
                days++;
            }
            date = date.plusDays(1);
        }
        return days;
    }

    private BigDecimal calculateDailyAverage(
            List<PurchaseSpending> historicalSpending,
            int historicalDays,
            BigDecimal currentSpending,
            int currentDays
    ) {
        BigDecimal historicalSpendingAmount = sum(historicalSpending);
        if (historicalDays > 0 && historicalSpendingAmount.compareTo(BigDecimal.ZERO) > 0) {
            return divide(historicalSpendingAmount, historicalDays);
        }
        if (currentDays > 0 && currentSpending.compareTo(BigDecimal.ZERO) > 0) {
            return divide(currentSpending, currentDays);
        }
        return BigDecimal.ZERO.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal sum(List<PurchaseSpending> spendingItems) {
        return spendingItems.stream()
                .map(PurchaseSpending::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal divide(BigDecimal amount, int divisor) {
        return amount.divide(BigDecimal.valueOf(divisor), AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePercentage(BigDecimal predictedSpending, BigDecimal limitAmount) {
        if (limitAmount == null || limitAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return predictedSpending
                .multiply(ONE_HUNDRED)
                .divide(limitAmount, AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateConfidence(
            List<PurchaseSpending> historicalSpending,
            List<PurchaseSpending> currentPeriodSpending
    ) {
        if (historicalSpending.isEmpty() && currentPeriodSpending.isEmpty()) {
            return NO_DATA_CONFIDENCE.setScale(CONFIDENCE_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal increment = BigDecimal.valueOf(historicalSpending.size())
                .divide(BigDecimal.valueOf(100), CONFIDENCE_SCALE, RoundingMode.HALF_UP);
        BigDecimal confidence = BASE_CONFIDENCE.add(increment);
        if (confidence.compareTo(MAX_CONFIDENCE) > 0) {
            return MAX_CONFIDENCE.setScale(CONFIDENCE_SCALE, RoundingMode.HALF_UP);
        }
        return confidence.setScale(CONFIDENCE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    private boolean isOperatingDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY
                && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }
}
