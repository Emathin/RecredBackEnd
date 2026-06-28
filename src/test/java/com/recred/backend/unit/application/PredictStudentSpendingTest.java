package com.recred.backend.unit.application;

import com.recred.backend.application.PredictStudentSpending;
import com.recred.backend.application.PredictStudentSpendingRequest;
import com.recred.backend.application.PredictStudentSpendingResponse;
import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.services.SpendingPredictionAnalyzer;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PurchaseSpendingRepository;
import com.recred.backend.domain.repositories.SpendingPreferenceRepository;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.SpendingPredictionAnalysis;
import com.recred.backend.domain.models.ConsumptionCategory;
import com.recred.backend.domain.models.PurchaseSpending;
import com.recred.backend.domain.models.SpendingPredictionResult;
import com.recred.backend.domain.models.SpendingPeriodPreference;
import com.recred.backend.domain.services.SpendingPredictionCalculator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private PredictStudentSpending predictStudentSpending;

    @BeforeEach
    void setUp() {
        predictStudentSpending = new PredictStudentSpending(
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
    void useActivePreferenceWhenPeriodIsNotRequested() {
        UUID studentId = UUID.randomUUID();
        givenExistingStudent(studentId);
        givenActivePreference(studentId, BudgetPeriod.QUINCENAL, "200.00");
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

        PredictStudentSpendingResponse response = whenSpendingIsPredicted(studentId, null);

        thenActivePreferencePredictionMatches(response);
        thenPreferenceWasQueried(studentId);
        thenPredictionWasRegisteredWithPeriod(BudgetPeriod.QUINCENAL);
    }

    @Test
    void requestedPeriodOverridesPreference() {
        UUID studentId = UUID.randomUUID();
        givenExistingStudent(studentId);
        givenActivePreference(studentId, BudgetPeriod.QUINCENAL, "200.00");
        givenNoSpendingData();

        PredictStudentSpendingResponse response = whenSpendingIsPredicted(
                studentId,
                BudgetPeriod.MENSUAL
        );

        thenPeriodMatches(response, BudgetPeriod.MENSUAL);
        thenPredictionWasRegistered();
    }

    @Test
    void resolveStudentIdFromUserIdWhenStudentIdDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        givenUserIdResolvesToStudent(userId, studentId);
        givenNoPreference();
        givenNoSpendingData();

        PredictStudentSpendingResponse response = whenSpendingIsPredicted(
                userId,
                BudgetPeriod.MENSUAL
        );

        thenStudentWasResolved(response, studentId);
        thenPreferenceWasQueried(studentId);
        thenCurrentRangeWasQueried(studentId);
    }

    @Test
    void useMonthlyPeriodWhenNoPreferenceAndNoRequestedPeriod() {
        UUID studentId = UUID.randomUUID();
        givenExistingStudent(studentId);
        givenNoPreference();
        givenNoSpendingData();

        PredictStudentSpendingResponse response = whenSpendingIsPredicted(studentId, null);

        thenPeriodMatches(response, BudgetPeriod.MENSUAL);
    }

    @Test
    void throwExceptionWhenStudentDoesNotExist() {
        UUID studentId = UUID.randomUUID();
        givenUnknownStudent(studentId);

        assertThrows(AlumnoNoEncontradoException.class, () -> whenSpendingIsPredicted(
                studentId,
                BudgetPeriod.MENSUAL
        ));
        thenPredictionDependenciesWereNotCalled();
    }

    @Test
    void querySpendingWithCurrentAndHistoricalRanges() {
        UUID studentId = UUID.randomUUID();
        givenExistingStudent(studentId);
        givenNoPreference();
        givenNoSpendingData();

        whenSpendingIsPredicted(studentId, BudgetPeriod.MENSUAL);

        thenCurrentRangeWasQueried(studentId);
        thenHistoricalRangeWasQueried(studentId);
    }

    private void givenExistingStudent(UUID studentId) {
        when(alumnoRepository.existsById(studentId)).thenReturn(true);
    }

    private void givenUnknownStudent(UUID studentId) {
        when(alumnoRepository.existsById(studentId)).thenReturn(false);
    }

    private void givenUserIdResolvesToStudent(UUID userId, UUID studentId) {
        when(alumnoRepository.existsById(userId)).thenReturn(false);
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
    }

    private void givenActivePreference(
            UUID studentId,
            BudgetPeriod period,
            String limitAmount
    ) {
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

    private void thenActivePreferencePredictionMatches(
            PredictStudentSpendingResponse response
    ) {
        assertEquals(BudgetPeriod.QUINCENAL, response.period());
        assertEquals(new BigDecimal("200.00"), response.limitAmount());
        assertEquals(5, response.mostConsumedCategories().size());
        assertEquals("Reducir snacks.", response.aiAnalysis().recommendations().get(0));
    }

    private void thenPeriodMatches(
            PredictStudentSpendingResponse response,
            BudgetPeriod period
    ) {
        assertEquals(period, response.period());
    }

    private void thenStudentWasResolved(
            PredictStudentSpendingResponse response,
            UUID studentId
    ) {
        assertEquals(studentId, response.studentId());
    }

    private void thenPreferenceWasQueried(UUID studentId) {
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

    private void thenCurrentRangeWasQueried(UUID studentId) {
        verify(purchaseSpendingRepository).findSpending(
                eq(studentId),
                anyCollection(),
                eq(LocalDateTime.parse("2026-05-01T00:00:00")),
                eq(LocalDateTime.parse("2026-05-21T23:59:59.999999999"))
        );
    }

    private void thenHistoricalRangeWasQueried(UUID studentId) {
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

    private PurchaseSpending spending(String dateTime, String amount) {
        return new PurchaseSpending(LocalDateTime.parse(dateTime), new BigDecimal(amount));
    }

    private PredictStudentSpendingResponse whenSpendingIsPredicted(
            UUID studentId,
            BudgetPeriod period
    ) {
        return predictStudentSpending.execute(new PredictStudentSpendingRequest(
                studentId,
                period
        ));
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
