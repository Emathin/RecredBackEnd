package com.recred.backend.application;

import com.recred.backend.config.HealthyRecommendationProperties;
import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.HealthyProductOption;
import com.recred.backend.domain.models.HealthyProductRecommendation;
import com.recred.backend.domain.models.HealthyRecommendationAnalysisRequest;
import com.recred.backend.domain.models.HealthyRecommendationMessages;
import com.recred.backend.domain.models.RecentProductConsumption;
import com.recred.backend.domain.repositories.ConsumptionAlertStudentRepository;
import com.recred.backend.domain.repositories.HealthyRecommendationNotificationRepository;
import com.recred.backend.domain.repositories.HealthyRecommendationPurchaseRepository;
import com.recred.backend.domain.services.HealthyRecommendationAnalyzer;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateHealthyProductRecommendationsTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-26T12:00:00Z"),
            ZoneId.of("UTC")
    );

    @Mock
    private ConsumptionAlertStudentRepository studentRepository;

    @Mock
    private HealthyRecommendationPurchaseRepository purchaseRepository;

    @Mock
    private HealthyRecommendationNotificationRepository notificationRepository;

    @Mock
    private HealthyRecommendationAnalyzer analyzer;

    private GenerateHealthyProductRecommendations useCase;

    @BeforeEach
    void setUp() {
        useCase = new GenerateHealthyProductRecommendations(
                studentRepository,
                purchaseRepository,
                notificationRepository,
                analyzer,
                properties(),
                CLOCK
        );
    }

    @Test
    void generateRecommendationWhenAiSelectsAvailableProductFromRecentHistory() {
        ConsumptionAlertCandidate candidate = candidate();
        UUID cokeId = UUID.randomUUID();
        UUID waterId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        when(studentRepository.findActiveCandidates()).thenReturn(List.of(candidate));
        when(notificationRepository.hasRecommendationSince(eq(candidate.studentId()), any()))
                .thenReturn(false);
        when(purchaseRepository.findConsumptions(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(
                        consumption(cokeId, "Coca Cola", "Gaseosa azucarada", null, buffetId,
                                "2026-05-24T10:00:00", null),
                        consumption(cokeId, "Coca Cola", "Gaseosa azucarada", null, buffetId,
                                "2026-05-25T10:00:00", 1)
                ));
        when(purchaseRepository.findAvailableOptions(
                eq(candidate.studentId()),
                eq(buffetId),
                eq(cokeId),
                anyCollection()
        )).thenReturn(List.of(
                option(waterId, "Agua mineral", "Agua sin azucar", null),
                option(UUID.randomUUID(), "Alfajor", "Dulce con chocolate", "Snacks")
        ));
        when(analyzer.analyze(any())).thenReturn(Optional.of(messages(waterId)));

        int generatedRecommendations = useCase.execute();

        assertEquals(1, generatedRecommendations);
        ArgumentCaptor<HealthyRecommendationAnalysisRequest> requestCaptor =
                ArgumentCaptor.forClass(HealthyRecommendationAnalysisRequest.class);
        verify(analyzer).analyze(requestCaptor.capture());
        HealthyRecommendationAnalysisRequest request = requestCaptor.getValue();
        assertEquals(2, request.consumptionHistory().size());
        assertEquals(2, request.availableOptions().size());

        ArgumentCaptor<HealthyProductRecommendation> recommendationCaptor =
                ArgumentCaptor.forClass(HealthyProductRecommendation.class);
        verify(notificationRepository).save(recommendationCaptor.capture());
        HealthyProductRecommendation recommendation = recommendationCaptor.getValue();
        assertEquals(cokeId, recommendation.pattern().repeatedProductId());
        assertEquals(waterId, recommendation.pattern().suggestedProductId());
        assertEquals(2, recommendation.pattern().repeatedDays());
        assertEquals(1L, recommendation.pattern().totalQuantity());
    }

    @Test
    void skipStudentInsideCooldown() {
        ConsumptionAlertCandidate candidate = candidate();
        when(studentRepository.findActiveCandidates()).thenReturn(List.of(candidate));
        when(notificationRepository.hasRecommendationSince(eq(candidate.studentId()), any()))
                .thenReturn(true);

        int generatedRecommendations = useCase.execute();

        assertEquals(0, generatedRecommendations);
        verify(purchaseRepository, never()).findConsumptions(
                any(),
                anyCollection(),
                any(),
                any()
        );
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void skipWhenProductWasNotRepeatedInEnoughDifferentDays() {
        ConsumptionAlertCandidate candidate = candidate();
        UUID cokeId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        when(studentRepository.findActiveCandidates()).thenReturn(List.of(candidate));
        when(notificationRepository.hasRecommendationSince(eq(candidate.studentId()), any()))
                .thenReturn(false);
        when(purchaseRepository.findConsumptions(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(
                        consumption(cokeId, "Coca Cola", "Gaseosa azucarada", "Bebidas",
                                buffetId, "2026-05-25T10:00:00", 1),
                        consumption(cokeId, "Coca Cola", "Gaseosa azucarada", "Bebidas",
                                buffetId, "2026-05-25T11:00:00", 1)
                ));

        int generatedRecommendations = useCase.execute();

        assertEquals(0, generatedRecommendations);
        verify(purchaseRepository, never()).findAvailableOptions(
                any(),
                any(),
                any(),
                anyCollection()
        );
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void skipWhenAiDoesNotRecommendAnything() {
        ConsumptionAlertCandidate candidate = candidate();
        UUID sandwichId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        when(studentRepository.findActiveCandidates()).thenReturn(List.of(candidate));
        when(notificationRepository.hasRecommendationSince(eq(candidate.studentId()), any()))
                .thenReturn(false);
        when(purchaseRepository.findConsumptions(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(
                        consumption(sandwichId, "Sandwich", "Pan con queso", "Comidas",
                                buffetId, "2026-05-24T10:00:00", 1),
                        consumption(sandwichId, "Sandwich", "Pan con queso", "Comidas",
                                buffetId, "2026-05-25T10:00:00", 1)
                ));
        when(purchaseRepository.findAvailableOptions(any(), any(), any(), anyCollection()))
                .thenReturn(List.of(option(UUID.randomUUID(), "Agua mineral",
                        "Agua sin azucar", "Bebidas")));
        when(analyzer.analyze(any(HealthyRecommendationAnalysisRequest.class)))
                .thenReturn(Optional.empty());

        int generatedRecommendations = useCase.execute();

        assertEquals(0, generatedRecommendations);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void skipWhenNoAvailableOptionExists() {
        ConsumptionAlertCandidate candidate = candidate();
        UUID cokeId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        when(studentRepository.findActiveCandidates()).thenReturn(List.of(candidate));
        when(notificationRepository.hasRecommendationSince(eq(candidate.studentId()), any()))
                .thenReturn(false);
        when(purchaseRepository.findConsumptions(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(
                        consumption(cokeId, "Coca Cola", "Gaseosa azucarada", "Bebidas",
                                buffetId, "2026-05-24T10:00:00", 1),
                        consumption(cokeId, "Coca Cola", "Gaseosa azucarada", "Bebidas",
                                buffetId, "2026-05-25T10:00:00", 1)
                ));
        when(purchaseRepository.findAvailableOptions(
                eq(candidate.studentId()),
                eq(buffetId),
                eq(cokeId),
                anyCollection()
        )).thenReturn(List.of());

        int generatedRecommendations = useCase.execute();

        assertEquals(0, generatedRecommendations);
        verify(analyzer, never()).analyze(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void skipWhenAiSelectsProductOutsideAvailableOptions() {
        ConsumptionAlertCandidate candidate = candidate();
        UUID cokeId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        when(studentRepository.findActiveCandidates()).thenReturn(List.of(candidate));
        when(notificationRepository.hasRecommendationSince(eq(candidate.studentId()), any()))
                .thenReturn(false);
        when(purchaseRepository.findConsumptions(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(
                        consumption(cokeId, "Coca Cola", "Gaseosa azucarada", "Bebidas",
                                buffetId, "2026-05-24T10:00:00", 1),
                        consumption(cokeId, "Coca Cola", "Gaseosa azucarada", "Bebidas",
                                buffetId, "2026-05-25T10:00:00", 1)
                ));
        when(purchaseRepository.findAvailableOptions(any(), any(), any(), anyCollection()))
                .thenReturn(List.of(option(UUID.randomUUID(), "Agua mineral",
                        "Agua sin azucar", "Bebidas")));
        when(analyzer.analyze(any(HealthyRecommendationAnalysisRequest.class)))
                .thenReturn(Optional.of(messages(UUID.randomUUID())));

        int generatedRecommendations = useCase.execute();

        assertEquals(0, generatedRecommendations);
        verify(notificationRepository, never()).save(any());
    }

    private ConsumptionAlertCandidate candidate() {
        return new ConsumptionAlertCandidate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Juan Perez",
                List.of()
        );
    }

    private RecentProductConsumption consumption(
            UUID productId,
            String productName,
            String description,
            String category,
            UUID buffetId,
            String purchaseDateTime,
            Integer quantity
    ) {
        return new RecentProductConsumption(
                productId,
                productName,
                description,
                category,
                buffetId,
                LocalDateTime.parse(purchaseDateTime),
                quantity
        );
    }

    private HealthyProductOption option(
            UUID productId,
            String productName,
            String description,
            String category
    ) {
        return new HealthyProductOption(productId, productName, description, category);
    }

    private HealthyRecommendationMessages messages(UUID suggestedProductId) {
        return new HealthyRecommendationMessages(
                "Proba agua",
                "Elegiste gaseosa estos dias. Hoy podes probar agua mineral.",
                "Elegir agua mineral.",
                suggestedProductId,
                new BigDecimal("0.8500"),
                "gemini-test"
        );
    }

    private HealthyRecommendationProperties properties() {
        return new HealthyRecommendationProperties(
                true,
                "0 30 9 * * *",
                2,
                2,
                3,
                10,
                20
        );
    }
}
