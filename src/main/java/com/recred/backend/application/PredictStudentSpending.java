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
    public PredictStudentSpendingResponse execute(PredictStudentSpendingRequest predictionRequest) {
        ensurePredictionRequestHasRequiredData(predictionRequest);
        UUID studentId = studentIdFromRequest(predictionRequest.studentId());
        SpendingPredictionContext context = predictionContextFor(
                studentId,
                predictionRequest.period());
        SpendingPredictionData data = predictionDataFor(context);
        SpendingPredictionResult result = predictionResultFrom(context, data);
        aiEventRepository.registerPrediction(result);
        return PredictStudentSpendingResponse.fromDomain(result);
    }

    private void ensurePredictionRequestHasRequiredData(
            PredictStudentSpendingRequest predictionRequest) {
        if (predictionRequest == null) {
            throw new IllegalArgumentException("request is required");
        }

        ensurePredictionRequestHasStudent(predictionRequest.studentId());
    }

    private void ensurePredictionRequestHasStudent(UUID studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("studentId is required");
        }
    }

    private SpendingPredictionContext predictionContextFor(
            UUID studentId,
            BudgetPeriod requestedPeriod
    ) {
        LocalDate calculationDate = LocalDate.now(clock);
        Optional<SpendingPeriodPreference> preference = activePreferenceFor(
                studentId,
                calculationDate);
        BudgetPeriod period = periodFromRequestOrPreference(requestedPeriod, preference);
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

    private SpendingPredictionData predictionDataFor(SpendingPredictionContext context) {
        return new SpendingPredictionData(
                currentPeriodSpendingFor(context),
                historicalSpendingFor(context),
                mostConsumedCategoriesFor(context)
        );
    }

    private List<PurchaseSpending> currentPeriodSpendingFor(
            SpendingPredictionContext context
    ) {
        return purchaseSpendingRepository.findSpending(
                context.studentId(),
                ACTUAL_SPENDING_STATUSES,
                startOfDay(context.currentRange().startDate()),
                endOfDay(context.calculationDate())
        );
    }

    private List<PurchaseSpending> historicalSpendingFor(
            SpendingPredictionContext context
    ) {
        return purchaseSpendingRepository.findSpending(
                context.studentId(),
                ACTUAL_SPENDING_STATUSES,
                startOfDay(context.historicalRange().startDate()),
                endOfDay(context.historicalRange().endDate())
        );
    }

    private List<ConsumptionCategory> mostConsumedCategoriesFor(
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

    private SpendingPredictionResult predictionResultFrom(
            SpendingPredictionContext context,
            SpendingPredictionData data
    ) {
        SpendingPrediction spendingPrediction = predictionFrom(context, data);
        Optional<SpendingPredictionAnalysis> aiAnalysis = spendingPredictionAnalyzer
                .analyze(spendingPrediction);
        return new SpendingPredictionResult(spendingPrediction, aiAnalysis);
    }

    private SpendingPrediction predictionFrom(
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

    private UUID studentIdFromRequest(UUID studentIdOrUserId) {
        return alumnoRepository.existsById(studentIdOrUserId)
                ? studentIdOrUserId
                : alumnoRepository.findStudentIdByUserId(studentIdOrUserId)
                        .orElseThrow(() -> new AlumnoNoEncontradoException(studentIdOrUserId));
    }

    private Optional<SpendingPeriodPreference> activePreferenceFor(
            UUID studentId,
            LocalDate calculationDate
    ) {
        return spendingPreferenceRepository
                .findActivePreferences(studentId, calculationDate)
                .stream()
                .findFirst();
    }

    private BudgetPeriod periodFromRequestOrPreference(
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
