package com.recred.backend.application;

import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.ConsumptionAlertTutor;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GeneratePurchaseRecommendation {

    private final AlumnoRepository alumnoRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseAnalyzer purchaseAnalyzer;
    private final AIEventRepository aiEventRepository;
    private final ConsumptionAlertStudentRepository studentRepository;
    private final PurchaseRecommendationNotificationRepository notificationRepository;
    private final SuggestionAiRepository suggestionAiRepository;

    public GeneratePurchaseRecommendation(
            AlumnoRepository alumnoRepository,
            PurchaseRepository purchaseRepository,
            PurchaseAnalyzer purchaseAnalyzer,
            AIEventRepository aiEventRepository,
            PurchaseRecommendationNotificationRepository notificationRepository,
            ConsumptionAlertStudentRepository studentRepository,
            SuggestionAiRepository suggestionAiRepository) {
        this.alumnoRepository = alumnoRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseAnalyzer = purchaseAnalyzer;
        this.aiEventRepository = aiEventRepository;
        this.notificationRepository = notificationRepository;
        this.studentRepository = studentRepository;
        this.suggestionAiRepository = suggestionAiRepository;
    }

    public GeneratePurchaseRecommendationResponse execute(
            GeneratePurchaseRecommendationRequest request
    ) {
        List<ConsumptionAlertCandidate> candidates = studentRepository.findActiveCandidates();
        candidates.stream()
                .filter(this::hasAdvancedTutor)
                .forEach(this::processCandidate);
        return new GeneratePurchaseRecommendationResponse();
    }

    private boolean hasAdvancedTutor(ConsumptionAlertCandidate candidate) {
        return candidate.tutors()
                .stream()
                .anyMatch(ConsumptionAlertTutor::hasAdvancedPlan);
    }

    private void processCandidate(ConsumptionAlertCandidate candidate) {
        UUID userId = alumnoRepository.findUserID(candidate.studentId());
        List<PurchaseResume> purchases = getPurchases(candidate.studentId());
        Optional<PurchasePatternAnalysis> analysis = purchaseAnalyzer.analyze(purchases);
        PurchaseRecommendationResult result =
                buildResult(candidate.studentId(), purchases, analysis);
        saveRecommendation(result, userId);
    }

    private List<PurchaseResume> getPurchases(UUID studentId) {
        return purchaseRepository.findPurchasesByAlumno(studentId);
    }

    private PurchaseRecommendationResult buildResult(
            UUID studentId,
            List<PurchaseResume> purchases,
            Optional<PurchasePatternAnalysis> analysis) {
        return new PurchaseRecommendationResult(studentId, purchases, analysis);
    }

    private void saveRecommendation(PurchaseRecommendationResult result, UUID userId) {
        UUID eventId = aiEventRepository.registerPurchaseRecommendation(result);
        var savedSuggestion = suggestionAiRepository.save(eventId, result);
        notificationRepository.save(result, savedSuggestion, userId);
    }

}
