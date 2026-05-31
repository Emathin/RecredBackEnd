package com.recred.backend.application;

import com.recred.backend.domain.models.PurchaseState;
import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.SpendingPredictionAnalysis;
import com.recred.backend.domain.exceptions.StudentNotFoundException;
import com.recred.backend.domain.models.ConsumptionCategory;
import com.recred.backend.domain.models.PurchaseSpending;
import com.recred.backend.domain.models.SpendingPrediction;
import com.recred.backend.domain.models.SpendingPredictionResult;
import com.recred.backend.domain.models.SpendingPeriodPreference;
import com.recred.backend.domain.models.DateRange;
import com.recred.backend.domain.repositories.StudentRepository;
import com.recred.backend.domain.repositories.PurchaseSpendingRepository;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.SpendingPreferenceRepository;
import com.recred.backend.domain.services.SpendingPredictionAnalyzer;
import com.recred.backend.domain.services.SpendingPredictionCalculator;
import com.recred.backend.domain.services.SpendingPredictionRangeService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class PredictStudentSpending {

    private static final int HISTORICAL_DAYS = 90;
    private static final int MAX_CATEGORIES = 5;
    private static final Set<PurchaseState> ACTUAL_SPENDING_STATUSES = Set.of(
            PurchaseState.ENTREGADO);

    private final StudentRepository studentRepository;
    private final SpendingPreferenceRepository spendingPreferenceRepository;
    private final PurchaseSpendingRepository purchaseSpendingRepository;
    private final SpendingPredictionAnalyzer spendingPredictionAnalyzer;
    private final AIEventRepository aiEventRepository;
    private final SpendingPredictionRangeService spendingPredictionRangeService;
    private final SpendingPredictionCalculator spendingPredictionCalculator;
    private final Clock clock;

    public PredictStudentSpending(
            StudentRepository studentRepository,
            SpendingPreferenceRepository spendingPreferenceRepository,
            PurchaseSpendingRepository purchaseSpendingRepository,
            SpendingPredictionAnalyzer spendingPredictionAnalyzer,
            AIEventRepository aiEventRepository,
            SpendingPredictionRangeService spendingPredictionRangeService,
            SpendingPredictionCalculator spendingPredictionCalculator,
            Clock clock
    ) {
        this.studentRepository = studentRepository;
        this.spendingPreferenceRepository = spendingPreferenceRepository;
        this.purchaseSpendingRepository = purchaseSpendingRepository;
        this.spendingPredictionAnalyzer = spendingPredictionAnalyzer;
        this.aiEventRepository = aiEventRepository;
        this.spendingPredictionRangeService = spendingPredictionRangeService;
        this.spendingPredictionCalculator = spendingPredictionCalculator;
        this.clock = clock;
    }

    @Transactional
    public SpendingPredictionResult execute(UUID studentId, BudgetPeriod requestedPeriod) {
        if (!studentRepository.existsById(studentId)) {
            throw new StudentNotFoundException(studentId);
        }

        LocalDate calculationDate = LocalDate.now(clock);
        Optional<SpendingPeriodPreference> preference = getPreference(studentId, calculationDate);
        BudgetPeriod period = resolvePeriod(requestedPeriod, preference);
        DateRange currentRange = spendingPredictionRangeService.calculate(period, calculationDate);
        DateRange historicalRange = new DateRange(
                currentRange.startDate().minusDays(HISTORICAL_DAYS),
                currentRange.startDate().minusDays(1)
        );

        List<PurchaseSpending> currentPeriodSpending = purchaseSpendingRepository.findSpending(
                studentId,
                ACTUAL_SPENDING_STATUSES,
                startOfDay(currentRange.startDate()),
                endOfDay(calculationDate)
        );
        List<PurchaseSpending> historicalSpending = purchaseSpendingRepository.findSpending(
                studentId,
                ACTUAL_SPENDING_STATUSES,
                startOfDay(historicalRange.startDate()),
                endOfDay(historicalRange.endDate())
        );
        List<ConsumptionCategory> categories = purchaseSpendingRepository
                .findMostConsumedCategories(
                        studentId,
                        ACTUAL_SPENDING_STATUSES,
                        startOfDay(currentRange.startDate()),
                        endOfDay(calculationDate)
                )
                .stream()
                .limit(MAX_CATEGORIES)
                .toList();

        SpendingPrediction spendingPrediction = spendingPredictionCalculator.calculate(
                studentId,
                period,
                calculationDate,
                currentRange,
                historicalRange,
                currentPeriodSpending,
                historicalSpending,
                preference,
                categories
        );
        Optional<SpendingPredictionAnalysis> aiAnalysis = spendingPredictionAnalyzer
                .analyze(spendingPrediction);
        SpendingPredictionResult result = new SpendingPredictionResult(
                spendingPrediction,
                aiAnalysis
        );
        aiEventRepository.registerPrediction(result);
        return result;
    }

    private Optional<SpendingPeriodPreference> getPreference(
            UUID studentId,
            LocalDate calculationDate
    ) {
        return spendingPreferenceRepository
                .findActivePreferences(studentId, calculationDate)
                .stream()
                .findFirst();
    }

    private BudgetPeriod resolvePeriod(
            BudgetPeriod requestedPeriod,
            Optional<SpendingPeriodPreference> preference
    ) {
        if (requestedPeriod != null) {
            return requestedPeriod;
        }
        return preference
                .map(SpendingPeriodPreference::period)
                .orElse(BudgetPeriod.MENSUAL);
    }

    private LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }
}
