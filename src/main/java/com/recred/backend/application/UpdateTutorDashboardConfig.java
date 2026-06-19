package com.recred.backend.application;

import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.TutorRepository;

public class UpdateTutorDashboardConfig {

    private final TutorRepository tutorRepository;

    public UpdateTutorDashboardConfig(TutorRepository tutorRepository) {
        this.tutorRepository = tutorRepository;
    }

    public void execute(String cognitoSubject, String dashboardConfig) {
        Tutor tutor = tutorRepository.findByUsuarioIdCognito(cognitoSubject)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tutor no encontrado para el usuario actual"));

        tutor.setDashboardConfig(dashboardConfig);
        tutorRepository.save(tutor);
    }
}
