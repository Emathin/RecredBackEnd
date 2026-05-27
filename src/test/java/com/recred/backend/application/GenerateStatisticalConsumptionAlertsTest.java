package com.recred.backend.application;

import com.recred.backend.config.StatisticalConsumptionAlertProperties;
import com.recred.backend.domain.models.ConsumptionAlert;
import com.recred.backend.domain.models.ConsumptionAlertAnalysisRequest;
import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.ConsumptionAlertMessages;
import com.recred.backend.domain.models.ConsumptionAlertTutor;
import com.recred.backend.domain.models.ConsumptionAlertType;
import com.recred.backend.domain.models.ConsumptionCategoryStatistic;
import com.recred.backend.domain.models.ConsumptionProductStatistic;
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
        when(studentRepository.findActiveCandidates()).thenReturn(List.of(candidate));
        when(notificationRepository.hasAnyAlertBetween(eq(candidate.studentId()), any(), any()))
                .thenReturn(false);
        when(purchaseRepository.countPurchases(any(), anyCollection(), any(), any()))
                .thenReturn(4L);
        when(notificationRepository.hasAlertSince(
                eq(candidate.studentId()),
                eq(ConsumptionAlertType.STATISTICAL_PATTERN),
                eq(LocalDateTime.parse("2026-05-18T00:00:00"))
        )).thenReturn(false);
        when(purchaseRepository.findCategoryStatistics(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(
                        category("Bebidas", "80.00"),
                        category("Snacks", "20.00")
                ));
        when(purchaseRepository.findProductStatistics(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(product(productId, "Agua", 3L)));
        when(analyzer.analyze(any())).thenReturn(Optional.of(messages()));

        int generatedAlerts = useCase.execute();

        assertEquals(1, generatedAlerts);
        ArgumentCaptor<ConsumptionAlert> captor = ArgumentCaptor.forClass(
                ConsumptionAlert.class);
        verify(notificationRepository).save(captor.capture());
        ConsumptionAlert alert = captor.getValue();
        assertEquals(ConsumptionAlertType.STATISTICAL_PATTERN, alert.pattern().type());
        assertEquals("Bebidas", alert.pattern().mainCategory());
        assertEquals(new BigDecimal("80.00"), alert.pattern().mainCategoryPercentage());
        assertEquals(productId, alert.pattern().suggestedProductId());
        assertEquals("Tomar mas agua.", alert.messages().suggestedAction());
    }

    @Test
    void generateHealthyRecommendationWhenStudentHasLittleHistory() {
        ConsumptionAlertCandidate candidate = candidate();
        when(studentRepository.findActiveCandidates()).thenReturn(List.of(candidate));
        when(notificationRepository.hasAnyAlertBetween(eq(candidate.studentId()), any(), any()))
                .thenReturn(false);
        when(purchaseRepository.countPurchases(any(), anyCollection(), any(), any()))
                .thenReturn(1L);
        when(notificationRepository.hasAlertSince(
                eq(candidate.studentId()),
                eq(ConsumptionAlertType.HEALTHY_RECOMMENDATION),
                eq(LocalDateTime.parse("2026-05-11T00:00:00"))
        )).thenReturn(false);
        when(analyzer.analyze(any())).thenReturn(Optional.of(messages()));

        int generatedAlerts = useCase.execute();

        assertEquals(1, generatedAlerts);
        ArgumentCaptor<ConsumptionAlert> captor = ArgumentCaptor.forClass(
                ConsumptionAlert.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(ConsumptionAlertType.HEALTHY_RECOMMENDATION, captor.getValue()
                .pattern().type());
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

    @Test
    void generatePatternWithoutCategoryOrSuggestedProductWhenStatsAreWeak() {
        ConsumptionAlertCandidate candidate = candidate();
        when(studentRepository.findActiveCandidates()).thenReturn(List.of(candidate));
        when(notificationRepository.hasAnyAlertBetween(eq(candidate.studentId()), any(), any()))
                .thenReturn(false);
        when(purchaseRepository.countPurchases(any(), anyCollection(), any(), any()))
                .thenReturn(3L);
        when(notificationRepository.hasAlertSince(
                eq(candidate.studentId()),
                eq(ConsumptionAlertType.STATISTICAL_PATTERN),
                any()
        )).thenReturn(false);
        when(purchaseRepository.findCategoryStatistics(any(), anyCollection(), any(), any()))
                .thenReturn(List.of());
        when(purchaseRepository.findProductStatistics(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(product(UUID.randomUUID(), "Agua", null)));
        when(analyzer.analyze(any())).thenReturn(Optional.of(messages()));

        int generatedAlerts = useCase.execute();

        assertEquals(1, generatedAlerts);
        ArgumentCaptor<ConsumptionAlert> captor = ArgumentCaptor.forClass(
                ConsumptionAlert.class);
        verify(notificationRepository).save(captor.capture());
        assertNull(captor.getValue().pattern().mainCategory());
        assertEquals(BigDecimal.ZERO, captor.getValue().pattern().mainCategoryPercentage());
        assertNull(captor.getValue().pattern().suggestedProductId());
    }

    @Test
    void generatePatternWithZeroAmountWhenCategoryAmountIsMissing() {
        ConsumptionAlertCandidate candidate = candidate();
        when(studentRepository.findActiveCandidates()).thenReturn(List.of(candidate));
        when(notificationRepository.hasAnyAlertBetween(eq(candidate.studentId()), any(), any()))
                .thenReturn(false);
        when(purchaseRepository.countPurchases(any(), anyCollection(), any(), any()))
                .thenReturn(3L);
        when(notificationRepository.hasAlertSince(
                eq(candidate.studentId()),
                eq(ConsumptionAlertType.STATISTICAL_PATTERN),
                any()
        )).thenReturn(false);
        when(purchaseRepository.findCategoryStatistics(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(category("Bebidas", null)));
        when(purchaseRepository.findProductStatistics(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(product(UUID.randomUUID(), "Agua", 1L)));
        when(analyzer.analyze(any())).thenReturn(Optional.of(messages()));

        int generatedAlerts = useCase.execute();

        assertEquals(1, generatedAlerts);
        ArgumentCaptor<ConsumptionAlert> captor = ArgumentCaptor.forClass(
                ConsumptionAlert.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals("Bebidas", captor.getValue().pattern().mainCategory());
        assertEquals(BigDecimal.ZERO, captor.getValue().pattern().totalAmount());
        assertEquals(BigDecimal.ZERO, captor.getValue().pattern().mainCategoryPercentage());
        assertNull(captor.getValue().pattern().suggestedProductId());
    }

    @Test
    void skipStudentWhenAlreadyHasAlertToday() {
        ConsumptionAlertCandidate candidate = candidate();
        when(studentRepository.findActiveCandidates()).thenReturn(List.of(candidate));
        when(notificationRepository.hasAnyAlertBetween(eq(candidate.studentId()), any(), any()))
                .thenReturn(true);

        int generatedAlerts = useCase.execute();

        assertEquals(0, generatedAlerts);
        verify(purchaseRepository, never()).countPurchases(any(), anyCollection(), any(), any());
        verify(analyzer, never()).analyze(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void skipStudentWhenAlertTypeIsInsideCooldown() {
        ConsumptionAlertCandidate candidate = candidate();
        when(studentRepository.findActiveCandidates()).thenReturn(List.of(candidate));
        when(notificationRepository.hasAnyAlertBetween(eq(candidate.studentId()), any(), any()))
                .thenReturn(false);
        when(purchaseRepository.countPurchases(any(), anyCollection(), any(), any()))
                .thenReturn(3L);
        when(notificationRepository.hasAlertSince(
                eq(candidate.studentId()),
                eq(ConsumptionAlertType.STATISTICAL_PATTERN),
                any()
        )).thenReturn(true);

        int generatedAlerts = useCase.execute();

        assertEquals(0, generatedAlerts);
        verify(analyzer, never()).analyze(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void skipStudentWhenAiDoesNotReturnMessages() {
        ConsumptionAlertCandidate candidate = candidate();
        when(studentRepository.findActiveCandidates()).thenReturn(List.of(candidate));
        when(notificationRepository.hasAnyAlertBetween(eq(candidate.studentId()), any(), any()))
                .thenReturn(false);
        when(purchaseRepository.countPurchases(any(), anyCollection(), any(), any()))
                .thenReturn(0L);
        when(notificationRepository.hasAlertSince(any(), any(), any())).thenReturn(false);
        when(analyzer.analyze(any(ConsumptionAlertAnalysisRequest.class)))
                .thenReturn(Optional.empty());

        int generatedAlerts = useCase.execute();

        assertEquals(0, generatedAlerts);
        verify(notificationRepository, never()).save(any());
    }

    private ConsumptionAlertCandidate candidate() {
        return new ConsumptionAlertCandidate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Juan Perez",
                List.of(new ConsumptionAlertTutor(UUID.randomUUID(), "Tutor Perez"))
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
