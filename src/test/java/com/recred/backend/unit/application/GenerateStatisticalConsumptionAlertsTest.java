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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Mock
    private ConsumptionAlertStudentRepository studentRepository;

    @Mock
    private ConsumptionAlertPurchaseRepository purchaseRepository;

    @Mock
    private ConsumptionAlertNotificationRepository notificationRepository;

    @Mock
    private ConsumptionAlertAnalyzer analyzer;

    private GenerateStatisticalConsumptionAlerts useCase;

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
    void generatePatternAlertWhenStudentHasEnoughHistory() {
        UUID productId = UUID.randomUUID();
        ConsumptionAlertCandidate candidate = candidate();
        givenActiveCandidate(candidate);
        givenStudentWithoutAlertToday(candidate);
        givenPurchaseCount(4L);
        givenStudentOutsideCooldown(
                candidate,
                ConsumptionAlertType.STATISTICAL_PATTERN,
                "2026-05-18T00:00:00"
        );
        givenCategoryStatistics(
                category("Bebidas", "80.00"),
                category("Snacks", "20.00")
        );
        givenProductStatistics(product(productId, "Agua", 3L));
        givenAiReturnsMessages();

        GenerateStatisticalConsumptionAlertsResponse response = whenGeneratingAlerts();

        thenGeneratedAlerts(response, 1);
        ConsumptionAlert alert = thenAlertWasSaved();
        assertEquals(ConsumptionAlertType.STATISTICAL_PATTERN, alert.pattern().type());
        assertEquals("Bebidas", alert.pattern().mainCategory());
        assertEquals(new BigDecimal("80.00"), alert.pattern().mainCategoryPercentage());
        assertEquals(productId, alert.pattern().suggestedProductId());
        assertEquals("Tomar mas agua.", alert.messages().suggestedAction());
    }

    @Test
    void generateHealthyRecommendationWhenStudentHasLittleHistory() {
        ConsumptionAlertCandidate candidate = candidate();
        givenActiveCandidate(candidate);
        givenStudentWithoutAlertToday(candidate);
        givenPurchaseCount(1L);
        givenStudentOutsideCooldown(
                candidate,
                ConsumptionAlertType.HEALTHY_RECOMMENDATION,
                "2026-05-11T00:00:00"
        );
        givenAiReturnsMessages();

        GenerateStatisticalConsumptionAlertsResponse response = whenGeneratingAlerts();

        thenGeneratedAlerts(response, 1);
        assertEquals(
                ConsumptionAlertType.HEALTHY_RECOMMENDATION,
                thenAlertWasSaved().pattern().type()
        );
        thenStatisticsWereNotRequested();
    }

    @Test
    void generatePatternWithoutCategoryOrSuggestedProductWhenStatsAreWeak() {
        ConsumptionAlertCandidate candidate = candidate();
        givenActiveCandidate(candidate);
        givenStudentWithoutAlertToday(candidate);
        givenPurchaseCount(3L);
        givenStudentOutsideCooldown(candidate, ConsumptionAlertType.STATISTICAL_PATTERN);
        givenCategoryStatistics();
        givenProductStatistics(product(UUID.randomUUID(), "Agua", null));
        givenAiReturnsMessages();

        GenerateStatisticalConsumptionAlertsResponse response = whenGeneratingAlerts();

        thenGeneratedAlerts(response, 1);
        ConsumptionAlert alert = thenAlertWasSaved();
        assertNull(alert.pattern().mainCategory());
        assertEquals(BigDecimal.ZERO, alert.pattern().mainCategoryPercentage());
        assertNull(alert.pattern().suggestedProductId());
    }

    @Test
    void generatePatternWithZeroAmountWhenCategoryAmountIsMissing() {
        ConsumptionAlertCandidate candidate = candidate();
        givenActiveCandidate(candidate);
        givenStudentWithoutAlertToday(candidate);
        givenPurchaseCount(3L);
        givenStudentOutsideCooldown(candidate, ConsumptionAlertType.STATISTICAL_PATTERN);
        givenCategoryStatistics(category("Bebidas", null));
        givenProductStatistics(product(UUID.randomUUID(), "Agua", 1L));
        givenAiReturnsMessages();

        GenerateStatisticalConsumptionAlertsResponse response = whenGeneratingAlerts();

        thenGeneratedAlerts(response, 1);
        ConsumptionAlert alert = thenAlertWasSaved();
        assertEquals("Bebidas", alert.pattern().mainCategory());
        assertEquals(BigDecimal.ZERO, alert.pattern().totalAmount());
        assertEquals(BigDecimal.ZERO, alert.pattern().mainCategoryPercentage());
        assertNull(alert.pattern().suggestedProductId());
    }

    @Test
    void skipStudentWhenAlreadyHasAlertToday() {
        ConsumptionAlertCandidate candidate = candidate();
        givenActiveCandidate(candidate);
        givenStudentWithAlertToday(candidate);

        GenerateStatisticalConsumptionAlertsResponse response = whenGeneratingAlerts();

        thenGeneratedAlerts(response, 0);
        thenPurchasesWereNotCounted();
        thenAnalyzerWasNotCalled();
        thenNoAlertWasSaved();
    }

    @Test
    void skipStudentWhenAlertTypeIsInsideCooldown() {
        ConsumptionAlertCandidate candidate = candidate();
        givenActiveCandidate(candidate);
        givenStudentWithoutAlertToday(candidate);
        givenPurchaseCount(3L);
        givenStudentInsideCooldown(candidate, ConsumptionAlertType.STATISTICAL_PATTERN);

        GenerateStatisticalConsumptionAlertsResponse response = whenGeneratingAlerts();

        thenGeneratedAlerts(response, 0);
        thenAnalyzerWasNotCalled();
        thenNoAlertWasSaved();
    }

    @Test
    void skipStudentWhenAiDoesNotReturnMessages() {
        ConsumptionAlertCandidate candidate = candidate();
        givenActiveCandidate(candidate);
        givenStudentWithoutAlertToday(candidate);
        givenPurchaseCount(0L);
        givenStudentOutsideCooldown(candidate, ConsumptionAlertType.HEALTHY_RECOMMENDATION);
        givenAiReturnsNoMessages();

        GenerateStatisticalConsumptionAlertsResponse response = whenGeneratingAlerts();

        thenGeneratedAlerts(response, 0);
        thenNoAlertWasSaved();
    }

    private void givenActiveCandidate(ConsumptionAlertCandidate candidate) {
        when(studentRepository.findActiveCandidates()).thenReturn(List.of(candidate));
    }

    private void givenStudentWithoutAlertToday(ConsumptionAlertCandidate candidate) {
        when(notificationRepository.hasAnyAlertBetween(eq(candidate.studentId()), any(), any()))
                .thenReturn(false);
    }

    private void givenStudentWithAlertToday(ConsumptionAlertCandidate candidate) {
        when(notificationRepository.hasAnyAlertBetween(eq(candidate.studentId()), any(), any()))
                .thenReturn(true);
    }

    private void givenPurchaseCount(long purchaseCount) {
        when(purchaseRepository.countPurchases(any(), anyCollection(), any(), any()))
                .thenReturn(purchaseCount);
    }

    private void givenStudentOutsideCooldown(
            ConsumptionAlertCandidate candidate,
            ConsumptionAlertType alertType
    ) {
        when(notificationRepository.hasAlertSince(
                eq(candidate.studentId()),
                eq(alertType),
                any()
        )).thenReturn(false);
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

    private void givenStudentInsideCooldown(
            ConsumptionAlertCandidate candidate,
            ConsumptionAlertType alertType
    ) {
        when(notificationRepository.hasAlertSince(
                eq(candidate.studentId()),
                eq(alertType),
                any()
        )).thenReturn(true);
    }

    private void givenCategoryStatistics(ConsumptionCategoryStatistic... statistics) {
        when(purchaseRepository.findCategoryStatistics(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(statistics));
    }

    private void givenProductStatistics(ConsumptionProductStatistic... statistics) {
        when(purchaseRepository.findProductStatistics(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(statistics));
    }

    private void givenAiReturnsMessages() {
        when(analyzer.analyze(any(ConsumptionAlertAnalysisRequest.class)))
                .thenReturn(Optional.of(messages()));
    }

    private void givenAiReturnsNoMessages() {
        when(analyzer.analyze(any(ConsumptionAlertAnalysisRequest.class)))
                .thenReturn(Optional.empty());
    }

    private GenerateStatisticalConsumptionAlertsResponse whenGeneratingAlerts() {
        return useCase.execute(new GenerateStatisticalConsumptionAlertsRequest());
    }

    private void thenGeneratedAlerts(
            GenerateStatisticalConsumptionAlertsResponse response,
            int expected
    ) {
        assertEquals(expected, response.generatedAlerts());
    }

    private ConsumptionAlert thenAlertWasSaved() {
        ArgumentCaptor<ConsumptionAlert> captor = ArgumentCaptor.forClass(
                ConsumptionAlert.class);
        verify(notificationRepository).save(captor.capture());
        return captor.getValue();
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

    private ConsumptionAlertCandidate candidate() {
        return new ConsumptionAlertCandidate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Juan Perez",
                List.of(new ConsumptionAlertTutor(
                        UUID.randomUUID(),
                        "Tutor Perez",
                        TipoPlan.AVANZADO
                ))
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

    private ConsumptionProductStatistic product(UUID productId, String name, Long quantity) {
        return new ConsumptionProductStatistic(
                productId,
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
