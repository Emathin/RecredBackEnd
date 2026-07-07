package com.recred.backend.unit.application;

import com.recred.backend.application.GeneratePurchaseRecommendation;
import com.recred.backend.application.GeneratePurchaseRecommendationRequest;
import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.PurchasePatternAnalysis;
import com.recred.backend.domain.models.PurchaseRecommendationResult;
import com.recred.backend.domain.models.PurchaseResume;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ConsumptionAlertStudentRepository;
import com.recred.backend.domain.repositories.PurchaseRecommendationNotificationRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.SuggestionAiRepository;
import com.recred.backend.domain.services.PurchaseAnalyzer;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeneratePurchaseRecommendationTest {

    private AlumnoRepository alumnoRepository;
    private PurchaseRepository purchaseRepository;
    private PurchaseAnalyzer purchaseAnalyzer;
    private AIEventRepository aiEventRepository;
    private PurchaseRecommendationNotificationRepository notificationRepository;
    private ConsumptionAlertStudentRepository studentRepository;
    private SuggestionAiRepository suggestionAiRepository;
    private GeneratePurchaseRecommendation useCase;

    private UUID studentId;
    private UUID userId;
    private UUID productId;
    private UUID eventId;
    private List<PurchaseResume> purchaseHistory;
    private PurchasePatternAnalysis analysis;
    private SugerenciaIAEntity savedSuggestion;
    private PurchaseRecommendationResult registeredRecommendation;

    @BeforeEach
    void setUp() {
        alumnoRepository = mock(AlumnoRepository.class);
        purchaseRepository = mock(PurchaseRepository.class);
        purchaseAnalyzer = mock(PurchaseAnalyzer.class);
        aiEventRepository = mock(AIEventRepository.class);
        notificationRepository = mock(PurchaseRecommendationNotificationRepository.class);
        studentRepository = mock(ConsumptionAlertStudentRepository.class);
        suggestionAiRepository = mock(SuggestionAiRepository.class);
        useCase = new GeneratePurchaseRecommendation(
                alumnoRepository,
                purchaseRepository,
                purchaseAnalyzer,
                aiEventRepository,
                notificationRepository,
                studentRepository,
                suggestionAiRepository
        );
    }

    @Test
    void givenActiveCandidate_whenGeneratingPurchaseRecommendation_thenSavesAndNotifiesRecommendation() {
        givenPurchaseRecommendationScenario();
        givenActiveStudentCandidate();
        givenUserIdForStudent();
        givenPurchaseHistory();
        givenPurchasePatternAnalysis();
        givenRecommendationEventIsRegistered();
        givenSuggestionIsSaved();

        whenGeneratingPurchaseRecommendations();

        thenPurchasePatternWasAnalyzed();
        thenRecommendationEventWasRegistered();
        thenSuggestionWasSaved();
        thenUserWasNotified();
    }

    private void givenPurchaseRecommendationScenario() {
        studentId = UUID.randomUUID();
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        savedSuggestion = new SugerenciaIAEntity();
        purchaseHistory = List.of(new PurchaseResume(
                studentId,
                productId,
                "Yogur",
                new BigDecimal("120.00"),
                LocalDateTime.of(2026, 6, 1, 10, 0)
        ));
        analysis = new PurchasePatternAnalysis(
                "Compra yogur los lunes",
                productId,
                "Yogur",
                "MONDAY"
        );
    }

    private void givenActiveStudentCandidate() {
        when(studentRepository.findActiveCandidates()).thenReturn(List.of(
                new ConsumptionAlertCandidate(studentId, userId, "Ana", List.of())
        ));
    }

    private void givenUserIdForStudent() {
        when(alumnoRepository.findUserID(studentId)).thenReturn(userId);
    }

    private void givenPurchaseHistory() {
        when(purchaseRepository.findPurchasesByAlumno(studentId)).thenReturn(purchaseHistory);
    }

    private void givenPurchasePatternAnalysis() {
        when(purchaseAnalyzer.analyze(purchaseHistory)).thenReturn(Optional.of(analysis));
    }

    private void givenRecommendationEventIsRegistered() {
        when(aiEventRepository.registerPurchaseRecommendation(any())).thenReturn(eventId);
    }

    private void givenSuggestionIsSaved() {
        when(suggestionAiRepository.save(any(), any())).thenReturn(savedSuggestion);
    }

    private void whenGeneratingPurchaseRecommendations() {
        useCase.execute(new GeneratePurchaseRecommendationRequest());
    }

    private void thenPurchasePatternWasAnalyzed() {
        verify(purchaseAnalyzer).analyze(purchaseHistory);
    }

    private void thenRecommendationEventWasRegistered() {
        registeredRecommendation = registeredRecommendation();
        assertEquals(studentId, registeredRecommendation.getAlumnoId());
        assertEquals(purchaseHistory, registeredRecommendation.getPurchases());
        assertEquals(Optional.of(analysis), registeredRecommendation.getAiAnalysis());
    }

    private void thenSuggestionWasSaved() {
        verify(suggestionAiRepository).save(eventId, registeredRecommendation);
    }

    private void thenUserWasNotified() {
        verify(notificationRepository).save(
                registeredRecommendation,
                savedSuggestion,
                userId);
    }

    private PurchaseRecommendationResult registeredRecommendation() {
        ArgumentCaptor<PurchaseRecommendationResult> resultCaptor =
                ArgumentCaptor.forClass(PurchaseRecommendationResult.class);
        verify(aiEventRepository).registerPurchaseRecommendation(resultCaptor.capture());
        return resultCaptor.getValue();
    }
}
