package com.recred.backend.application;

import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.PurchasePatternAnalysis;
import com.recred.backend.domain.models.PurchaseResume;
import com.recred.backend.domain.repositories.AIEventRepository;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ConsumptionAlertStudentRepository;
import com.recred.backend.domain.repositories.PurchaseRecommendationNotificationRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.SuggestionAiRepository;
import com.recred.backend.domain.services.PurchaseAnalyzer;
import com.recred.backend.domain.models.PurchaseRecommendationResult;
import com.recred.backend.infrastructure.entities.NotificacionEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GeneratePurchaseRecommendation {

    private AlumnoRepository alumnoRepository;
    private PurchaseRepository purchaseRepository;
    private PurchaseAnalyzer purchaseAnalyzer;
    private final AIEventRepository aiEventRepository;
    private final ConsumptionAlertStudentRepository studentRepository;

    private final PurchaseRecommendationNotificationRepository notificationRepository;
    private final SuggestionAiRepository suggestionAiRepository;


    public GeneratePurchaseRecommendation(
            AlumnoRepository alumnoRepository, PurchaseRepository purchaseRepository,
            PurchaseAnalyzer purchaseAnalyzer, AIEventRepository aiEventRepository,
            PurchaseRecommendationNotificationRepository notificationRepository,
            ConsumptionAlertStudentRepository studentRepository,
            SuggestionAiRepository suggestionAiRepository) {
        this.alumnoRepository = alumnoRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseAnalyzer = purchaseAnalyzer;
        this.aiEventRepository = aiEventRepository;
        this.studentRepository = studentRepository;
        this.notificationRepository = notificationRepository;
        this.suggestionAiRepository = suggestionAiRepository;
    }

    public void execute() {
        for (ConsumptionAlertCandidate candidate : studentRepository.findActiveCandidates()) {
            UUID userID = this.alumnoRepository.findUserID(candidate.studentId());
            List<PurchaseResume> purchases =
                    this.purchaseRepository.findPurchasesByAlumno(candidate.studentId());
            Optional<PurchasePatternAnalysis> aiAnalysis = purchaseAnalyzer.analyze(purchases);

            PurchaseRecommendationResult result =
                    new PurchaseRecommendationResult(candidate.studentId(),
                            purchases, aiAnalysis);
            UUID eventID = aiEventRepository.registerPurchaseRecommendation(result);

            notificationRepository.save(
                    result,
                    suggestionAiRepository.save(eventID,result),
                    userID);
        }
    }

    public NotificacionEntity findSuggestionForToday(UUID studentID) {
        String today = LocalDate.now().getDayOfWeek().name();
        return suggestionAiRepository.findByTipoAndStudentId(today, studentID);
    }
    public NotificacionEntity findSuggestionForTomorrow(UUID studentID) {
        String today = LocalDate.now().plusDays(1).getDayOfWeek().name();
        return suggestionAiRepository.findByTipoAndStudentId(today, studentID);
    }
}
