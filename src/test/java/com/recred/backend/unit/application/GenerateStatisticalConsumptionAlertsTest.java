package com.recred.backend.unit.application;

import com.recred.backend.application.GenerateStatisticalConsumptionAlerts;
import com.recred.backend.application.GenerateStatisticalConsumptionAlertsRequest;
import com.recred.backend.application.GenerateStatisticalConsumptionAlertsResponse;
import com.recred.backend.config.StatisticalConsumptionAlertProperties;
import com.recred.backend.domain.models.ConsumptionAlert;
import com.recred.backend.domain.models.ConsumptionAlertAnalysisRequest;
import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.ConsumptionAlertMessages;
import com.recred.backend.domain.models.ConsumptionAlertTutor;
import com.recred.backend.domain.models.ConsumptionAlertType;
import com.recred.backend.domain.models.ConsumptionCategoryStatistic;
import com.recred.backend.domain.models.ConsumptionProductStatistic;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.repositories.ConsumptionAlertNotificationRepository;
import com.recred.backend.domain.repositories.ConsumptionAlertPurchaseRepository;
import com.recred.backend.domain.repositories.ConsumptionAlertStudentRepository;
import com.recred.backend.domain.services.ConsumptionAlertAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
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

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateStatisticalConsumptionAlertsTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-25T12:00:00Z"),
            ZoneId.of("UTC")
    );
    private static final String STATISTICAL_PATTERN_COOLDOWN_START = "2026-05-18T00:00:00";
    private static final String HEALTHY_RECOMMENDATION_COOLDOWN_START = "2026-05-11T00:00:00";

    @Mock
    private ConsumptionAlertStudentRepository studentRepository;

    @Mock
    private ConsumptionAlertPurchaseRepository purchaseRepository;

    @Mock
    private ConsumptionAlertNotificationRepository notificationRepository;

    @Mock
    private ConsumptionAlertAnalyzer analyzer;

    private GenerateStatisticalConsumptionAlerts useCase;
    private GenerateStatisticalConsumptionAlertsResponse response;
    private RuntimeException thrownException;
    private ConsumptionAlertCandidate candidate;
    private ConsumptionAlert savedAlert;
    private UUID productId;

    @BeforeEach
    void setUp() {
        useCase = new GenerateStatisticalConsumptionAlerts(
                studentRepository,
                purchaseRepository,
                notificationRepository,
                analyzer,
                properties(),
                CLOCK
        );
    }

    @Test
    void givenStudentWithEnoughHistory_whenGeneratingAlerts_thenSavesPatternAlert() {
        givenStudentWithStatisticalPattern();
        givenCategoryConsumptionIsMostlyBeverages();
        givenStrongProductSuggestion();
        givenAiReturnsMessages();

        whenGeneratingAlerts();

        thenOneAlertWasGenerated();
        thenStatisticalPatternAlertWasSaved();
    }

    @Test
    void givenStudentWithLittleHistory_whenGeneratingAlerts_thenSavesHealthyRecommendation() {
        givenStudentNeedsHealthyRecommendation();
        givenAiReturnsMessages();

        whenGeneratingAlerts();

        thenOneAlertWasGenerated();
        thenHealthyRecommendationAlertWasSaved();
        thenStatisticsWereNotRequested();
    }

    @Test
    void givenWeakStatistics_whenGeneratingAlerts_thenSavesPatternWithoutSuggestion() {
        givenStudentWithStatisticalPattern();
        givenNoRelevantCategoryConsumption();
        givenWeakProductSuggestion();
        givenAiReturnsMessages();

        whenGeneratingAlerts();

        thenOneAlertWasGenerated();
        thenPatternWasSavedWithoutSuggestion();
    }

    @Test
    void givenMissingCategoryAmount_whenGeneratingAlerts_thenSavesPatternWithZeroAmount() {
        givenStudentWithStatisticalPattern();
        givenCategoryConsumptionWithoutAmount();
        givenWeakProductSuggestion();
        givenAiReturnsMessages();

        whenGeneratingAlerts();

        thenOneAlertWasGenerated();
        thenPatternWasSavedWithZeroCategoryAmount();
    }

    @Test
    void givenStudentAlreadyAlertedToday_whenGeneratingAlerts_thenSkipsStudent() {
        givenStudentAlreadyAlertedToday();

        whenGeneratingAlerts();

        thenNoAlertsWereGenerated();
        thenPurchasesWereNotCounted();
        thenAnalyzerWasNotCalled();
        thenNoAlertWasSaved();
    }

    @Test
    void givenAlertTypeInsideCooldown_whenGeneratingAlerts_thenSkipsStudent() {
        givenStudentInsideStatisticalPatternCooldown();

        whenGeneratingAlerts();

        thenNoAlertsWereGenerated();
        thenAnalyzerWasNotCalled();
        thenNoAlertWasSaved();
    }

    @Test
    void givenAiDoesNotReturnMessages_whenGeneratingAlerts_thenDoesNotSaveAlert() {
        givenStudentNeedsHealthyRecommendation();
        givenAiReturnsNoMessages();

        whenGeneratingAlerts();

        thenNoAlertsWereGenerated();
        thenNoAlertWasSaved();
    }

    @Test
    void givenMissingRequest_whenGeneratingAlerts_thenThrowsException() {
        whenGeneratingAlertsWithoutRequest();

        thenErrorMessageIs("request is required");
    }

    private void givenStudentWithStatisticalPattern() {
        givenActiveStudentCandidate();
        givenStudentWithoutAlertToday();
        givenPurchaseCount(4L);
        givenStudentOutsideCooldown(
                ConsumptionAlertType.STATISTICAL_PATTERN,
                STATISTICAL_PATTERN_COOLDOWN_START
        );
    }

    private void givenStudentNeedsHealthyRecommendation() {
        givenActiveStudentCandidate();
        givenStudentWithoutAlertToday();
        givenPurchaseCount(1L);
        givenStudentOutsideCooldown(
                ConsumptionAlertType.HEALTHY_RECOMMENDATION,
                HEALTHY_RECOMMENDATION_COOLDOWN_START
        );
    }

    private void givenStudentAlreadyAlertedToday() {
        givenActiveStudentCandidate();
        when(notificationRepository.hasAnyAlertBetween(
                eq(candidate.studentId()),
                any(),
                any()
        )).thenReturn(true);
    }

    private void givenStudentInsideStatisticalPatternCooldown() {
        givenActiveStudentCandidate();
        givenStudentWithoutAlertToday();
        givenPurchaseCount(3L);
        when(notificationRepository.hasAlertSince(
                eq(candidate.studentId()),
                eq(ConsumptionAlertType.STATISTICAL_PATTERN),
                any()
        )).thenReturn(true);
    }

    private void givenActiveStudentCandidate() {
        candidate = candidate();
        when(studentRepository.findActiveCandidates()).thenReturn(List.of(candidate));
    }

    private void givenStudentWithoutAlertToday() {
        when(notificationRepository.hasAnyAlertBetween(
                eq(candidate.studentId()),
                any(),
                any()
        )).thenReturn(false);
    }

    private void givenPurchaseCount(long purchaseCount) {
        when(purchaseRepository.countPurchases(any(), anyCollection(), any(), any()))
                .thenReturn(purchaseCount);
    }

    private void givenStudentOutsideCooldown(
            ConsumptionAlertCandidate candidate,
            ConsumptionAlertType alertType,
            String cooldownStartDateTime
    ) {
        when(notificationRepository.hasAlertSince(
                eq(candidate.studentId()),
                eq(alertType),
                eq(LocalDateTime.parse(cooldownStartDateTime))
        )).thenReturn(false);
    }

    private void givenCategoryConsumptionIsMostlyBeverages() {
        when(purchaseRepository.findCategoryStatistics(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(
                        category("Bebidas", "80.00"),
                        category("Snacks", "20.00")
                ));
    }

    private void givenNoRelevantCategoryConsumption() {
        when(purchaseRepository.findCategoryStatistics(any(), anyCollection(), any(), any()))
                .thenReturn(List.of());
    }

    private void givenCategoryConsumptionWithoutAmount() {
        when(purchaseRepository.findCategoryStatistics(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(category("Bebidas", null)));
    }

    private void givenStrongProductSuggestion() {
        productId = randomUUID();
        when(purchaseRepository.findProductStatistics(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(product(productId, "Agua", 3L)));
    }

    private void givenWeakProductSuggestion() {
        productId = randomUUID();
        when(purchaseRepository.findProductStatistics(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(product(productId, "Agua", 1L)));
    }

    private void givenAiReturnsMessages() {
        when(analyzer.analyze(any(ConsumptionAlertAnalysisRequest.class)))
                .thenReturn(Optional.of(messages()));
    }

    private void givenAiReturnsNoMessages() {
        when(analyzer.analyze(any(ConsumptionAlertAnalysisRequest.class)))
                .thenReturn(Optional.empty());
    }

    private void whenGeneratingAlerts() {
        response = useCase.execute(new GenerateStatisticalConsumptionAlertsRequest());
    }

    private void whenGeneratingAlertsWithoutRequest() {
        thrownException = assertThrows(RuntimeException.class, () -> useCase.execute(null));
    }

    private void thenOneAlertWasGenerated() {
        assertEquals(1, response.generatedAlerts());
    }

    private void thenNoAlertsWereGenerated() {
        assertEquals(0, response.generatedAlerts());
    }

    private void thenStatisticalPatternAlertWasSaved() {
        savedAlert = savedAlert();
        assertEquals(ConsumptionAlertType.STATISTICAL_PATTERN, savedAlert.pattern().type());
        assertEquals("Bebidas", savedAlert.pattern().mainCategory());
        assertEquals(new BigDecimal("80.00"), savedAlert.pattern().mainCategoryPercentage());
        assertEquals(productId, savedAlert.pattern().suggestedProductId());
        assertEquals("Tomar mas agua.", savedAlert.messages().suggestedAction());
    }

    private void thenHealthyRecommendationAlertWasSaved() {
        savedAlert = savedAlert();
        assertEquals(
                ConsumptionAlertType.HEALTHY_RECOMMENDATION,
                savedAlert.pattern().type()
        );
    }

    private void thenPatternWasSavedWithoutSuggestion() {
        savedAlert = savedAlert();
        assertNull(savedAlert.pattern().mainCategory());
        assertEquals(BigDecimal.ZERO, savedAlert.pattern().mainCategoryPercentage());
        assertNull(savedAlert.pattern().suggestedProductId());
    }

    private void thenPatternWasSavedWithZeroCategoryAmount() {
        savedAlert = savedAlert();
        assertEquals("Bebidas", savedAlert.pattern().mainCategory());
        assertEquals(BigDecimal.ZERO, savedAlert.pattern().totalAmount());
        assertEquals(BigDecimal.ZERO, savedAlert.pattern().mainCategoryPercentage());
        assertNull(savedAlert.pattern().suggestedProductId());
    }

    private void thenStatisticsWereNotRequested() {
        verify(purchaseRepository, never()).findCategoryStatistics(
                any(),
                anyCollection(),
                any(),
                any()
        );
        verify(purchaseRepository, never()).findProductStatistics(
                any(),
                anyCollection(),
                any(),
                any()
        );
    }

    private void thenPurchasesWereNotCounted() {
        verify(purchaseRepository, never()).countPurchases(any(), anyCollection(), any(), any());
    }

    private void thenAnalyzerWasNotCalled() {
        verify(analyzer, never()).analyze(any());
    }

    private void thenNoAlertWasSaved() {
        verify(notificationRepository, never()).save(any());
    }

    private void thenErrorMessageIs(String message) {
        assertEquals(message, thrownException.getMessage());
    }

    private ConsumptionAlert savedAlert() {
        ArgumentCaptor<ConsumptionAlert> captor = ArgumentCaptor.forClass(
                ConsumptionAlert.class);
        verify(notificationRepository).save(captor.capture());
        return captor.getValue();
    }

    private ConsumptionAlertCandidate candidate() {
        return new ConsumptionAlertCandidate(
                randomUUID(),
                randomUUID(),
                "Juan Perez",
                List.of(new ConsumptionAlertTutor(
                        UUID.randomUUID(),
                        "Tutor Perez",
                        TipoPlan.AVANZADO
                ))
                List.of(new ConsumptionAlertTutor(randomUUID(), "Tutor Perez"))
        );
    }

    private ConsumptionCategoryStatistic category(String category, String amount) {
        return new ConsumptionCategoryStatistic(
                category,
                2L,
                2L,
                amount == null ? null : new BigDecimal(amount)
        );
    }

    private ConsumptionProductStatistic product(UUID requestedProductId, String name, Long quantity) {
        return new ConsumptionProductStatistic(
                requestedProductId,
                name,
                "Bebidas",
                2L,
                quantity,
                new BigDecimal("80.00")
        );
    }

    private ConsumptionAlertMessages messages() {
        return new ConsumptionAlertMessages(
                "Elegir agua",
                "Sugerencia para Juan",
                "Te recomendamos tomar mas agua.",
                "Recomendamos sugerir a Juan que tome mas agua.",
                "Tomar mas agua.",
                new BigDecimal("0.8000"),
                "gemini-test"
        );
    }

    private StatisticalConsumptionAlertProperties properties() {
        return new StatisticalConsumptionAlertProperties(
                true,
                "0 0 9 * * *",
                30,
                3,
                7,
                14,
                2L
        );
    }
}
