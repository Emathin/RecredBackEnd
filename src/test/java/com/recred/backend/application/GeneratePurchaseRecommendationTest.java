package com.recred.backend.application;

import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.PurchasePatternAnalysis;
import com.recred.backend.domain.models.PurchaseRecommendationResult;
import com.recred.backend.domain.models.PurchaseResume;
import com.recred.backend.domain.repositories.*;
import com.recred.backend.domain.services.PurchaseAnalyzer;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GeneratePurchaseRecommendationTest {

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private PurchaseAnalyzer purchaseAnalyzer;
    @Mock
    private AIEventRepository aiEventRepository;
    @Mock
    private ConsumptionAlertStudentRepository studentRepository;
    @Mock
    private PurchaseRecommendationNotificationRepository notificationRepository;
    @Mock
    private SuggestionAiRepository suggestionAiRepository;

    private GeneratePurchaseRecommendation generatePurchaseRecommendation;

    @BeforeEach
    void setUp() {
        generatePurchaseRecommendation = new GeneratePurchaseRecommendation(
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
    void execute_WhenNoActiveCandidates_ShouldDoNothing() {

        when(studentRepository.findActiveCandidates()).thenReturn(List.of());

        generatePurchaseRecommendation.execute();

        verify(studentRepository).findActiveCandidates();
        verifyNoInteractions(alumnoRepository);
        verifyNoInteractions(purchaseRepository);
        verifyNoInteractions(purchaseAnalyzer);
        verifyNoInteractions(aiEventRepository);
        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(suggestionAiRepository);
    }

    @Test
    void execute_WhenActiveCandidate_ShouldGenerateRecommendation() {

        UUID studentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        ConsumptionAlertCandidate candidate = new ConsumptionAlertCandidate(studentId, userId, "Student Name", List.of());

        when(studentRepository.findActiveCandidates()).thenReturn(List.of(candidate));
        when(alumnoRepository.findUserID(studentId)).thenReturn(userId);

        List<PurchaseResume> purchases = List.of(mock(PurchaseResume.class));
        when(purchaseRepository.findPurchasesByAlumno(studentId)).thenReturn(purchases);

        PurchasePatternAnalysis analysis = new PurchasePatternAnalysis(
                "Summary", UUID.randomUUID(), "Product", "MONDAY");
        when(purchaseAnalyzer.analyze(purchases)).thenReturn(Optional.of(analysis));

        when(aiEventRepository.registerPurchaseRecommendation(any(PurchaseRecommendationResult.class))).thenReturn(eventId);

        SugerenciaIAEntity suggestionEntity = mock(SugerenciaIAEntity.class);
        when(suggestionAiRepository.save(eq(eventId), any(PurchaseRecommendationResult.class))).thenReturn(suggestionEntity);


        generatePurchaseRecommendation.execute();

        verify(studentRepository).findActiveCandidates();
        verify(alumnoRepository).findUserID(studentId);
        verify(purchaseRepository).findPurchasesByAlumno(studentId);
        verify(purchaseAnalyzer).analyze(purchases);
        verify(aiEventRepository).registerPurchaseRecommendation(any(PurchaseRecommendationResult.class));
        verify(suggestionAiRepository).save(eq(eventId), any(PurchaseRecommendationResult.class));
        verify(notificationRepository).save(any(PurchaseRecommendationResult.class), eq(suggestionEntity), eq(userId));
    }
}
