package com.recred.backend.unit.application;

import com.recred.backend.application.PredictStudentSpending;
import com.recred.backend.application.PredictStudentSpendingRequest;
import com.recred.backend.application.PredictStudentSpendingResponse;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.ConsumptionCategory;
import com.recred.backend.domain.models.PurchaseSpending;
import com.recred.backend.domain.models.SpendingPeriodPreference;
import com.recred.backend.domain.models.SpendingPredictionAnalysis;
import com.recred.backend.domain.models.SpendingPredictionResult;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PurchaseSpendingRepository;
import com.recred.backend.domain.repositories.SpendingPreferenceRepository;
import com.recred.backend.domain.services.SpendingPredictionAnalyzer;
import com.recred.backend.domain.services.SpendingPredictionCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PredictStudentSpendingTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-21T12:00:00Z"),
            ZoneId.of("UTC")
    );

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private SpendingPreferenceRepository spendingPreferenceRepository;

    @Mock
    private PurchaseSpendingRepository purchaseSpendingRepository;

    @Mock
    private SpendingPredictionAnalyzer spendingPredictionAnalyzer;

    @Mock
    private AIEventRepository aiEventRepository;

    private PredictStudentSpending useCase;

    private UUID studentId;
    private UUID userId;
    private PredictStudentSpendingResponse response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new PredictStudentSpending(
                alumnoRepository,
                spendingPreferenceRepository,
                purchaseSpendingRepository,
                spendingPredictionAnalyzer,
                aiEventRepository,
                new SpendingPredictionCalculator(),
                CLOCK
        );
    }

    @Test
    void givenActivePreferenceAndNoRequestedPeriod_whenPredictingSpending_thenUsesPreference() {
        givenPredictionScenario();
        givenExistingStudent();
        givenActivePreference(BudgetPeriod.QUINCENAL, "200.00");
        givenCurrentAndHistoricalSpending(
                List.of(spending("2026-05-21T10:00:00", "50.00")),
                List.of(spending("2026-04-10T10:00:00", "100.00"))
        );
        givenMostConsumedCategories(
                category("Bebidas", "20.00"),
                category("Snacks", "15.00"),
                category("Frutas", "10.00"),
                category("Panificados", "8.00"),
                category("Golosinas", "5.00"),
                category("Otros", "1.00")
        );
        givenAiAnalysis();

        whenPredictingSpendingWithoutRequestedPeriod();

        thenActivePreferencePredictionIsReturned();
        thenPreferenceWasQueriedForStudent();
        thenPredictionWasRegisteredWithPeriod(BudgetPeriod.QUINCENAL);
    }

    @Test
    void givenRequestedPeriodAndActivePreference_whenPredictingSpending_thenRequestedPeriodWins() {
        givenPredictionScenario();
        givenExistingStudent();
        givenActivePreference(BudgetPeriod.QUINCENAL, "200.00");
        givenNoSpendingData();

        whenPredictingSpendingFor(BudgetPeriod.MENSUAL);

        thenPeriodIs(BudgetPeriod.MENSUAL);
        thenPredictionWasRegistered();
    }

    @Test
    void givenUserIdBelongsToStudent_whenPredictingSpending_thenUsesResolvedStudent() {
        givenUserPredictionScenario();
        givenUserIdResolvesToStudent();
        givenNoPreference();
        givenNoSpendingData();

        whenPredictingSpendingForUser(BudgetPeriod.MENSUAL);

        thenStudentWasResolved();
        thenPreferenceWasQueriedForStudent();
        thenCurrentRangeWasQueriedForStudent();
    }

    @Test
    void givenNoPreferenceAndNoRequestedPeriod_whenPredictingSpending_thenUsesMonthlyPeriod() {
        givenPredictionScenario();
        givenExistingStudent();
        givenNoPreference();
        givenNoSpendingData();

        whenPredictingSpendingWithoutRequestedPeriod();

        thenPeriodIs(BudgetPeriod.MENSUAL);
    }

    @Test
    void givenUnknownStudent_whenPredictingSpending_thenFailsBeforeQueryingPredictionData() {
        givenPredictionScenario();
        givenUnknownStudent();

        whenPredictingSpendingFailsFor(BudgetPeriod.MENSUAL);

        thenStudentWasNotFound();
        thenPredictionDependenciesWereNotCalled();
    }

    @Test
    void givenExistingStudent_whenPredictingSpending_thenQueriesCurrentAndHistoricalRanges() {
        givenPredictionScenario();
        givenExistingStudent();
        givenNoPreference();
        givenNoSpendingData();

        whenPredictingSpendingFor(BudgetPeriod.MENSUAL);

        thenCurrentRangeWasQueriedForStudent();
        thenHistoricalRangeWasQueriedForStudent();
    }

    private void givenPredictionScenario() {
        studentId = UUID.randomUUID();
    }

    private void givenUserPredictionScenario() {
        userId = UUID.randomUUID();
        studentId = UUID.randomUUID();
    }

    private void givenExistingStudent() {
        when(alumnoRepository.existsById(studentId)).thenReturn(true);
    }

    private void givenUnknownStudent() {
        when(alumnoRepository.existsById(studentId)).thenReturn(false);
        when(alumnoRepository.findStudentIdByUserId(studentId)).thenReturn(Optional.empty());
    }

    private void givenUserIdResolvesToStudent() {
        when(alumnoRepository.existsById(userId)).thenReturn(false);
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
    }

    private void givenActivePreference(BudgetPeriod period, String limitAmount) {
        when(spendingPreferenceRepository.findActivePreferences(
                studentId,
                LocalDate.of(2026, 5, 21)
        )).thenReturn(List.of(new SpendingPeriodPreference(
                period,
                new BigDecimal(limitAmount)
        )));
    }

    private void givenNoPreference() {
        when(spendingPreferenceRepository.findActivePreferences(any(), any()))
                .thenReturn(List.of());
    }

    private void givenNoSpendingData() {
        givenCurrentAndHistoricalSpending(List.of(), List.of());
        givenMostConsumedCategories();
        givenNoAiAnalysis();
    }

    private void givenCurrentAndHistoricalSpending(
            List<PurchaseSpending> currentSpending,
            List<PurchaseSpending> historicalSpending
    ) {
        when(purchaseSpendingRepository.findSpending(any(), anyCollection(), any(), any()))
                .thenReturn(currentSpending, historicalSpending);
    }

    private void givenMostConsumedCategories(ConsumptionCategory... categories) {
        when(purchaseSpendingRepository.findMostConsumedCategories(
                any(),
                anyCollection(),
                any(),
                any()
        )).thenReturn(List.of(categories));
    }

    private void givenAiAnalysis() {
        when(spendingPredictionAnalyzer.analyze(any())).thenReturn(Optional.of(analysis()));
    }

    private void givenNoAiAnalysis() {
        when(spendingPredictionAnalyzer.analyze(any())).thenReturn(Optional.empty());
    }

    private void whenPredictingSpendingWithoutRequestedPeriod() {
        whenPredictingSpendingFor(null);
    }

    private void whenPredictingSpendingFor(BudgetPeriod period) {
        response = useCase.execute(request(studentId, period));
    }

    private void whenPredictingSpendingForUser(BudgetPeriod period) {
        response = useCase.execute(request(userId, period));
    }

    private void whenPredictingSpendingFailsFor(BudgetPeriod period) {
        thrownException = assertThrows(RuntimeException.class,
                () -> useCase.execute(request(studentId, period)));
    }

    private void thenActivePreferencePredictionIsReturned() {
        assertEquals(BudgetPeriod.QUINCENAL, response.period());
        assertEquals(new BigDecimal("200.00"), response.limitAmount());
        assertEquals(5, response.mostConsumedCategories().size());
        assertEquals("Reducir snacks.", response.aiAnalysis().recommendations().get(0));
    }

    private void thenPeriodIs(BudgetPeriod period) {
        assertEquals(period, response.period());
    }

    private void thenStudentWasResolved() {
        assertEquals(studentId, response.studentId());
    }

    private void thenStudentWasNotFound() {
        assertTrue(thrownException instanceof AlumnoNoEncontradoException);
    }

    private void thenPreferenceWasQueriedForStudent() {
        verify(spendingPreferenceRepository).findActivePreferences(
                studentId,
                LocalDate.of(2026, 5, 21)
        );
    }

    private void thenPredictionWasRegistered() {
        verify(aiEventRepository).registerPrediction(any(SpendingPredictionResult.class));
    }

    private void thenPredictionWasRegisteredWithPeriod(BudgetPeriod period) {
        ArgumentCaptor<SpendingPredictionResult> captor = ArgumentCaptor.forClass(
                SpendingPredictionResult.class);
        verify(aiEventRepository).registerPrediction(captor.capture());
        assertEquals(period, captor.getValue().spendingPrediction().period());
    }

    private void thenCurrentRangeWasQueriedForStudent() {
        verify(purchaseSpendingRepository).findSpending(
                eq(studentId),
                anyCollection(),
                eq(LocalDateTime.parse("2026-05-01T00:00:00")),
                eq(LocalDateTime.parse("2026-05-21T23:59:59.999999999"))
        );
    }

    private void thenHistoricalRangeWasQueriedForStudent() {
        verify(purchaseSpendingRepository).findSpending(
                eq(studentId),
                anyCollection(),
                eq(LocalDateTime.parse("2026-01-31T00:00:00")),
                eq(LocalDateTime.parse("2026-04-30T23:59:59.999999999"))
        );
    }

    private void thenPredictionDependenciesWereNotCalled() {
        verify(spendingPreferenceRepository, never()).findActivePreferences(any(), any());
        verify(purchaseSpendingRepository, never()).findSpending(
                any(),
                anyCollection(),
                any(),
                any()
        );
        verify(spendingPredictionAnalyzer, never()).analyze(any());
        verify(aiEventRepository, never()).registerPrediction(any());
    }

    private PredictStudentSpendingRequest request(UUID studentId, BudgetPeriod period) {
        return new PredictStudentSpendingRequest(studentId, period);
    }

    private PurchaseSpending spending(String dateTime, String amount) {
        return new PurchaseSpending(LocalDateTime.parse(dateTime), new BigDecimal(amount));
    }

    private ConsumptionCategory category(String description, String amount) {
        return new ConsumptionCategory(description, new BigDecimal(amount));
    }

    private SpendingPredictionAnalysis analysis() {
        return new SpendingPredictionAnalysis(
                "Puede superar el presupuesto.",
                List.of("Alerta de gasto."),
                List.of("Reducir snacks."),
                "gemini-test"
        );
    }
}
