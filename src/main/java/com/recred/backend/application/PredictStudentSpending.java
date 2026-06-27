package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.SpendingPredictionAnalysis;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.ConsumptionCategory;
import com.recred.backend.domain.models.PurchaseSpending;
import com.recred.backend.domain.models.SpendingPrediction;
import com.recred.backend.domain.models.SpendingPredictionResult;
import com.recred.backend.domain.models.SpendingPeriodPreference;
import com.recred.backend.domain.models.DateRange;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PurchaseSpendingRepository;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.SpendingPreferenceRepository;
import com.recred.backend.domain.services.SpendingPredictionAnalyzer;
import com.recred.backend.domain.services.SpendingPredictionCalculator;
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
    public PredictStudentSpendingResponse execute(PredictStudentSpendingRequest request) {
        requireRequest(request);
        UUID studentId = resolvedStudentId(request.studentId());
        SpendingPredictionContext context = spendingPredictionContext(studentId, request.period());
        SpendingPredictionData data = spendingPredictionData(context);
        SpendingPredictionResult result = spendingPredictionResult(context, data);
        aiEventRepository.registerPrediction(result);
        return PredictStudentSpendingResponse.fromDomain(result);
    }

    private void requireRequest(PredictStudentSpendingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        requireStudentId(request.studentId());
    }

    private void requireStudentId(UUID studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("studentId is required");
        }
    }

    private SpendingPredictionContext spendingPredictionContext(
            UUID studentId,
            BudgetPeriod requestedPeriod
    ) {
        LocalDate calculationDate = LocalDate.now(clock);
        Optional<SpendingPeriodPreference> preference = getPreference(studentId, calculationDate);
        BudgetPeriod period = resolvePeriod(requestedPeriod, preference);
        DateRange currentRange = DateRange.fromBudgetPeriod(period, calculationDate);
        DateRange historicalRange = historicalRange(currentRange);
        return new SpendingPredictionContext(
                studentId,
                period,
                calculationDate,
                preference,
                currentRange,
                historicalRange
        );
    }

    private DateRange historicalRange(DateRange currentRange) {
        return new DateRange(
                currentRange.startDate().minusDays(HISTORICAL_DAYS),
                currentRange.startDate().minusDays(1)
        );
    }

    private SpendingPredictionData spendingPredictionData(SpendingPredictionContext context) {
        return new SpendingPredictionData(
                currentPeriodSpending(context),
                historicalSpending(context),
                mostConsumedCategories(context)
        );
    }

    private List<PurchaseSpending> currentPeriodSpending(
            SpendingPredictionContext context
    ) {
        return purchaseSpendingRepository.findSpending(
                context.studentId(),
                ACTUAL_SPENDING_STATUSES,
                startOfDay(context.currentRange().startDate()),
                endOfDay(context.calculationDate())
        );
    }

    private List<PurchaseSpending> historicalSpending(
            SpendingPredictionContext context
    ) {
        return purchaseSpendingRepository.findSpending(
                context.studentId(),
                ACTUAL_SPENDING_STATUSES,
                startOfDay(context.historicalRange().startDate()),
                endOfDay(context.historicalRange().endDate())
        );
    }

    private List<ConsumptionCategory> mostConsumedCategories(
            SpendingPredictionContext context
    ) {
        return purchaseSpendingRepository
                .findMostConsumedCategories(
                        context.studentId(),
                        ACTUAL_SPENDING_STATUSES,
                        startOfDay(context.currentRange().startDate()),
                        endOfDay(context.calculationDate())
                )
                .stream()
                .limit(MAX_CATEGORIES)
                .toList();
    }

    private SpendingPredictionResult spendingPredictionResult(
            SpendingPredictionContext context,
            SpendingPredictionData data
    ) {
        SpendingPrediction spendingPrediction = spendingPrediction(context, data);
        Optional<SpendingPredictionAnalysis> aiAnalysis = spendingPredictionAnalyzer
                .analyze(spendingPrediction);
        return new SpendingPredictionResult(spendingPrediction, aiAnalysis);
    }

    private SpendingPrediction spendingPrediction(
            SpendingPredictionContext context,
            SpendingPredictionData data
    ) {
        return spendingPredictionCalculator.calculate(
                context.studentId(),
                context.period(),
                context.calculationDate(),
                context.currentRange(),
                context.historicalRange(),
                data.currentPeriodSpending(),
                data.historicalSpending(),
                context.preference(),
                data.categories()
        );
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

    private record SpendingPredictionContext(
            UUID studentId,
            BudgetPeriod period,
            LocalDate calculationDate,
            Optional<SpendingPeriodPreference> preference,
            DateRange currentRange,
            DateRange historicalRange
    ) {
    }

    private record SpendingPredictionData(
            List<PurchaseSpending> currentPeriodSpending,
            List<PurchaseSpending> historicalSpending,
            List<ConsumptionCategory> categories
    ) {
    }
}
