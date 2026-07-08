package com.recred.backend.application;

import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.TutorRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UpdateTutorDashboardConfig {

    private final TutorRepository tutorRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UpdateTutorDashboardConfig(TutorRepository tutorRepository) {
        this.tutorRepository = tutorRepository;
    }

    public UpdateTutorDashboardConfigResponse execute(UpdateTutorDashboardConfigRequest request) {
        request.validate();

        Tutor tutor = findTutor(request.cognitoSubject());
        validatePlanLimits(tutor, request.dashboardConfig());

        tutor.setDashboardConfig(request.dashboardConfig());
        tutorRepository.save(tutor);
        return new UpdateTutorDashboardConfigResponse();
    }

    private Tutor findTutor(String cognitoSubject) {
        return tutorRepository.findByUsuarioIdCognito(cognitoSubject)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tutor no encontrado para el usuario actual"));
    }

    private void validatePlanLimits(Tutor tutor, String dashboardConfig) {
        if (tutor.getUsuario() != null && tutor.getUsuario().hasFreePlan()) {
            int cardsCount = countCards(dashboardConfig);
            if (cardsCount > 5) {
                throw new IllegalArgumentException(
                        "El plan gratuito permite hasta 5 tarjetas en el dashboard");
            }
        }
    }

    private int countCards(String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return 0;
        }
        try {
            JsonNode root = objectMapper.readTree(configJson);
            if (root.isArray()) {
                return root.size();
            } else if (root.isObject()) {
                String[] arrayKeys = {"tarjetas", "cards", "widgets", "components"};
                for (String key : arrayKeys) {
                    if (root.has(key) && root.get(key).isArray()) {
                        return root.get(key).size();
                    }
                }
                return root.size();
            }
        } catch (Exception e) {
        }
        return 0;
    }
}
