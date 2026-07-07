package com.recred.backend.unit.application;

import com.recred.backend.application.GenerateHealthyProductRecommendations;
import com.recred.backend.application.GenerateHealthyProductRecommendationsRequest;
import com.recred.backend.application.GenerateHealthyProductRecommendationsResponse;
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
    void givenRepeatedProductAndAiSuggestedAvailableAlternative_whenGeneratingRecommendations_thenSavesRecommendation() {
        ConsumptionAlertCandidate candidate = candidate();
        UUID cokeId = UUID.randomUUID();
        UUID waterId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        givenActiveCandidate(candidate);
        givenStudentOutsideCooldown(candidate);
        givenRecentConsumptions(
                consumption(cokeId, "Coca Cola", "Gaseosa azucarada", null, buffetId,
                        "2026-05-24T10:00:00", null),
                consumption(cokeId, "Coca Cola", "Gaseosa azucarada", null, buffetId,
                        "2026-05-25T10:00:00", 1)
        );
        givenAvailableOptions(candidate, buffetId, cokeId,
                option(waterId, "Agua mineral", "Agua sin azucar", null),
                option(UUID.randomUUID(), "Alfajor", "Dulce con chocolate", "Snacks")
        );
        givenAiSuggests(waterId);

        GenerateHealthyProductRecommendationsResponse response = whenGeneratingRecommendations();

        thenGeneratedRecommendations(response, 1);
        thenAnalyzerReceivesRequest(2, 2);
        thenRecommendationWasSaved(cokeId, waterId, 2, 1L);
    }

    @Test
    void givenStudentInsideCooldown_whenGeneratingRecommendations_thenSkipsStudent() {
        ConsumptionAlertCandidate candidate = candidate();
        givenActiveCandidate(candidate);
        givenStudentInsideCooldown(candidate);

        GenerateHealthyProductRecommendationsResponse response = whenGeneratingRecommendations();

        thenGeneratedRecommendations(response, 0);
        thenConsumptionsWereNotRequested();
        thenNoRecommendationWasSaved();
    }

    @Test
    void givenProductRepeatedOnlySameDay_whenGeneratingRecommendations_thenSkipsStudent() {
        ConsumptionAlertCandidate candidate = candidate();
        UUID cokeId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        givenActiveCandidate(candidate);
        givenStudentOutsideCooldown(candidate);
        givenRecentConsumptions(
                consumption(cokeId, "Coca Cola", "Gaseosa azucarada", "Bebidas",
                        buffetId, "2026-05-25T10:00:00", 1),
                consumption(cokeId, "Coca Cola", "Gaseosa azucarada", "Bebidas",
                        buffetId, "2026-05-25T11:00:00", 1)
        );

        GenerateHealthyProductRecommendationsResponse response = whenGeneratingRecommendations();

        thenGeneratedRecommendations(response, 0);
        thenAvailableOptionsWereNotRequested();
        thenNoRecommendationWasSaved();
    }

    @Test
    void givenAiDoesNotRecommendAlternative_whenGeneratingRecommendations_thenSkipsStudent() {
        ConsumptionAlertCandidate candidate = candidate();
        UUID sandwichId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        givenActiveCandidate(candidate);
        givenStudentOutsideCooldown(candidate);
        givenRecentConsumptions(
                consumption(sandwichId, "Sandwich", "Pan con queso", "Comidas",
                        buffetId, "2026-05-24T10:00:00", 1),
                consumption(sandwichId, "Sandwich", "Pan con queso", "Comidas",
                        buffetId, "2026-05-25T10:00:00", 1)
        );
        givenAvailableOptions(
                option(UUID.randomUUID(), "Agua mineral", "Agua sin azucar", "Bebidas")
        );
        givenAiDoesNotRecommendAnything();

        GenerateHealthyProductRecommendationsResponse response = whenGeneratingRecommendations();

        thenGeneratedRecommendations(response, 0);
        thenNoRecommendationWasSaved();
    }

    @Test
    void givenNoAvailableAlternative_whenGeneratingRecommendations_thenSkipsStudent() {
        ConsumptionAlertCandidate candidate = candidate();
        UUID cokeId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        givenActiveCandidate(candidate);
        givenStudentOutsideCooldown(candidate);
        givenRecentConsumptions(
                consumption(cokeId, "Coca Cola", "Gaseosa azucarada", "Bebidas",
                        buffetId, "2026-05-24T10:00:00", 1),
                consumption(cokeId, "Coca Cola", "Gaseosa azucarada", "Bebidas",
                        buffetId, "2026-05-25T10:00:00", 1)
        );
        givenAvailableOptions(candidate, buffetId, cokeId);

        GenerateHealthyProductRecommendationsResponse response = whenGeneratingRecommendations();

        thenGeneratedRecommendations(response, 0);
        thenAnalyzerWasNotCalled();
        thenNoRecommendationWasSaved();
    }

    @Test
    void givenAiSelectsUnavailableAlternative_whenGeneratingRecommendations_thenSkipsStudent() {
        ConsumptionAlertCandidate candidate = candidate();
        UUID cokeId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        givenActiveCandidate(candidate);
        givenStudentOutsideCooldown(candidate);
        givenRecentConsumptions(
                consumption(cokeId, "Coca Cola", "Gaseosa azucarada", "Bebidas",
                        buffetId, "2026-05-24T10:00:00", 1),
                consumption(cokeId, "Coca Cola", "Gaseosa azucarada", "Bebidas",
                        buffetId, "2026-05-25T10:00:00", 1)
        );
        givenAvailableOptions(
                option(UUID.randomUUID(), "Agua mineral", "Agua sin azucar", "Bebidas")
        );
        givenAiSuggests(UUID.randomUUID());

        GenerateHealthyProductRecommendationsResponse response = whenGeneratingRecommendations();

        thenGeneratedRecommendations(response, 0);
        thenNoRecommendationWasSaved();
    }

    private void givenActiveCandidate(ConsumptionAlertCandidate candidate) {
        when(studentRepository.findActiveCandidates()).thenReturn(List.of(candidate));
    }

    private void givenStudentOutsideCooldown(ConsumptionAlertCandidate candidate) {
        when(notificationRepository.hasRecommendationSince(eq(candidate.studentId()), any()))
                .thenReturn(false);
    }

    private void givenStudentInsideCooldown(ConsumptionAlertCandidate candidate) {
        when(notificationRepository.hasRecommendationSince(eq(candidate.studentId()), any()))
                .thenReturn(true);
    }

    private void givenRecentConsumptions(RecentProductConsumption... consumptions) {
        when(purchaseRepository.findConsumptions(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(consumptions));
    }

    private void givenAvailableOptions(HealthyProductOption... options) {
        when(purchaseRepository.findAvailableOptions(any(), any(), any(), anyCollection()))
                .thenReturn(List.of(options));
    }

    private void givenAvailableOptions(
            ConsumptionAlertCandidate candidate,
            UUID buffetId,
            UUID excludedProductId,
            HealthyProductOption... options
    ) {
        when(purchaseRepository.findAvailableOptions(
                eq(candidate.studentId()),
                eq(buffetId),
                eq(excludedProductId),
                anyCollection()
        )).thenReturn(List.of(options));
    }

    private void givenAiSuggests(UUID productId) {
        when(analyzer.analyze(any(HealthyRecommendationAnalysisRequest.class)))
                .thenReturn(Optional.of(messages(productId)));
    }

    private void givenAiDoesNotRecommendAnything() {
        when(analyzer.analyze(any(HealthyRecommendationAnalysisRequest.class)))
                .thenReturn(Optional.empty());
    }

    private GenerateHealthyProductRecommendationsResponse whenGeneratingRecommendations() {
        return useCase.execute(new GenerateHealthyProductRecommendationsRequest());
    }

    private void thenGeneratedRecommendations(
            GenerateHealthyProductRecommendationsResponse response,
            int expected
    ) {
        assertEquals(expected, response.generatedRecommendations());
    }

    private void thenAnalyzerReceivesRequest(
            int expectedHistorySize,
            int expectedAvailableOptionsSize
    ) {
        ArgumentCaptor<HealthyRecommendationAnalysisRequest> requestCaptor =
                ArgumentCaptor.forClass(HealthyRecommendationAnalysisRequest.class);
        verify(analyzer).analyze(requestCaptor.capture());
        HealthyRecommendationAnalysisRequest request = requestCaptor.getValue();
        assertEquals(expectedHistorySize, request.consumptionHistory().size());
        assertEquals(expectedAvailableOptionsSize, request.availableOptions().size());
    }

    private void thenRecommendationWasSaved(
            UUID expectedRepeatedProductId,
            UUID expectedSuggestedProductId,
            int expectedRepeatedDays,
            long expectedTotalQuantity
    ) {
        ArgumentCaptor<HealthyProductRecommendation> recommendationCaptor =
                ArgumentCaptor.forClass(HealthyProductRecommendation.class);
        verify(notificationRepository).save(recommendationCaptor.capture());
        HealthyProductRecommendation recommendation = recommendationCaptor.getValue();
        assertEquals(expectedRepeatedProductId, recommendation.pattern().repeatedProductId());
        assertEquals(expectedSuggestedProductId, recommendation.pattern().suggestedProductId());
        assertEquals(expectedRepeatedDays, recommendation.pattern().repeatedDays());
        assertEquals(expectedTotalQuantity, recommendation.pattern().totalQuantity());
    }

    private void thenNoRecommendationWasSaved() {
        verify(notificationRepository, never()).save(any());
    }

    private void thenConsumptionsWereNotRequested() {
        verify(purchaseRepository, never()).findConsumptions(
                any(),
                anyCollection(),
                any(),
                any()
        );
    }

    private void thenAvailableOptionsWereNotRequested() {
        verify(purchaseRepository, never()).findAvailableOptions(
                any(),
                any(),
                any(),
                anyCollection()
        );
    }

    private void thenAnalyzerWasNotCalled() {
        verify(analyzer, never()).analyze(any());
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
