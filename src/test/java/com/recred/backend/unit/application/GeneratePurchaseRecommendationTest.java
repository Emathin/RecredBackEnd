package com.recred.backend.unit.application;

import com.recred.backend.application.*;

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
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    void executeGeneratesRecommendationForEachActiveCandidate() {
        UUID studentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        SugerenciaIAEntity suggestion = new SugerenciaIAEntity();
        List<PurchaseResume> purchases = List.of(new PurchaseResume(
                studentId,
                productId,
                "Yogur",
                new BigDecimal("120.00"),
                LocalDateTime.of(2026, 6, 1, 10, 0)
        ));
        PurchasePatternAnalysis analysis = new PurchasePatternAnalysis(
                "Compra yogur los lunes",
                productId,
                "Yogur",
                "MONDAY"
        );
        when(studentRepository.findActiveCandidates()).thenReturn(List.of(
                new ConsumptionAlertCandidate(studentId, userId, "Ana", List.of())
        ));
        when(alumnoRepository.findUserID(studentId)).thenReturn(userId);
        when(purchaseRepository.findPurchasesByAlumno(studentId)).thenReturn(purchases);
        when(purchaseAnalyzer.analyze(purchases)).thenReturn(Optional.of(analysis));
        when(aiEventRepository.registerPurchaseRecommendation(any())).thenReturn(eventId);
        when(suggestionAiRepository.save(any(), any())).thenReturn(suggestion);

        useCase.execute(new GeneratePurchaseRecommendationRequest());

        ArgumentCaptor<PurchaseRecommendationResult> resultCaptor =
                ArgumentCaptor.forClass(PurchaseRecommendationResult.class);
        verify(aiEventRepository).registerPurchaseRecommendation(resultCaptor.capture());
        PurchaseRecommendationResult result = resultCaptor.getValue();
        assertEquals(studentId, result.getAlumnoId());
        assertEquals(purchases, result.getPurchases());
        assertEquals(Optional.of(analysis), result.getAiAnalysis());
        verify(suggestionAiRepository).save(eventId, result);
        verify(notificationRepository).save(result, suggestion, userId);
    }

}
