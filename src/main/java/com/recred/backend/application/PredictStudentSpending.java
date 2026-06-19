package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.ConsumptionCategory;
import com.recred.backend.domain.models.DateRange;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.PurchaseSpending;
import com.recred.backend.domain.models.SpendingPeriodPreference;
import com.recred.backend.domain.models.SpendingPrediction;
import com.recred.backend.domain.models.SpendingPredictionAnalysis;
import com.recred.backend.domain.models.SpendingPredictionResult;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PurchaseSpendingRepository;
import com.recred.backend.domain.repositories.SpendingPreferenceRepository;
import com.recred.backend.domain.services.SpendingPredictionAnalyzer;
import com.recred.backend.domain.services.SpendingPredictionCalculator;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PredictStudentSpending {

    private static final int HISTORICAL_DAYS = 90;
    private static final int MAX_CATEGORIES = 5;
    private static final Set<EstadoCompra> ACTUAL_SPENDING_STATUSES = Set.of(
            EstadoCompra.ENTREGADO);

    private final AlumnoRepository alumnoRepository;
    private final SpendingPreferenceRepository spendingPreferenceRepository;
    private final PurchaseSpendingRepository purchaseSpendingRepository;
    private final SpendingPredictionAnalyzer spendingPredictionAnalyzer;
    private final AIEventRepository aiEventRepository;
    private final SpendingPredictionCalculator spendingPredictionCalculator;
    private final Clock clock;

    public PredictStudentSpending(
            AlumnoRepository alumnoRepository,
            SpendingPreferenceRepository spendingPreferenceRepository,
            PurchaseSpendingRepository purchaseSpendingRepository,
            SpendingPredictionAnalyzer spendingPredictionAnalyzer,
            AIEventRepository aiEventRepository,
            SpendingPredictionCalculator spendingPredictionCalculator,
            Clock clock
    ) {
        this.alumnoRepository = alumnoRepository;
        this.spendingPreferenceRepository = spendingPreferenceRepository;
        this.purchaseSpendingRepository = purchaseSpendingRepository;
        this.spendingPredictionAnalyzer = spendingPredictionAnalyzer;
        this.aiEventRepository = aiEventRepository;
        this.spendingPredictionCalculator = spendingPredictionCalculator;
        this.clock = clock;
    }

    @Transactional
    public SpendingPredictionResult execute(UUID studentId, BudgetPeriod requestedPeriod) {
        UUID resolvedStudentId = resolvedStudentId(studentId);

        LocalDate calculationDate = LocalDate.now(clock);
        Optional<SpendingPeriodPreference> preference = getPreference(
                resolvedStudentId,
                calculationDate
        );
        BudgetPeriod period = resolvePeriod(requestedPeriod, preference);
        DateRange currentRange = DateRange.fromBudgetPeriod(period, calculationDate);
        DateRange historicalRange = new DateRange(
                currentRange.startDate().minusDays(HISTORICAL_DAYS),
                currentRange.startDate().minusDays(1)
        );

        List<PurchaseSpending> currentPeriodSpending = purchaseSpendingRepository.findSpending(
                resolvedStudentId,
                ACTUAL_SPENDING_STATUSES,
                startOfDay(currentRange.startDate()),
                endOfDay(calculationDate)
        );
        List<PurchaseSpending> historicalSpending = purchaseSpendingRepository.findSpending(
                resolvedStudentId,
                ACTUAL_SPENDING_STATUSES,
                startOfDay(historicalRange.startDate()),
                endOfDay(historicalRange.endDate())
        );
        List<ConsumptionCategory> categories = purchaseSpendingRepository
                .findMostConsumedCategories(
                        resolvedStudentId,
                        ACTUAL_SPENDING_STATUSES,
                        startOfDay(currentRange.startDate()),
                        endOfDay(calculationDate)
                )
                .stream()
                .limit(MAX_CATEGORIES)
                .toList();

        SpendingPrediction spendingPrediction = spendingPredictionCalculator.calculate(
                resolvedStudentId,
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

    private UUID resolvedStudentId(UUID studentIdOrUserId) {
        return alumnoRepository.existsById(studentIdOrUserId)
                ? studentIdOrUserId
                : alumnoRepository.findStudentIdByUserId(studentIdOrUserId)
                        .orElseThrow(() -> new AlumnoNoEncontradoException(studentIdOrUserId));
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
