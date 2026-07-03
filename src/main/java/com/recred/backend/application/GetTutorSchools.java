package com.recred.backend.application;

import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.TutorSchoolRepository;

import java.util.List;

public class GetTutorSchools {

    private final TutorRepository tutorRepository;
    private final TutorSchoolRepository tutorSchoolRepository;

    public GetTutorSchools(TutorRepository tutorRepository,
                           TutorSchoolRepository tutorSchoolRepository) {
        this.tutorRepository = tutorRepository;
        this.tutorSchoolRepository = tutorSchoolRepository;
    }

    public List<Colegio> execute(String cognitoSubject) {
        Tutor tutor = tutorRepository.findByUsuarioIdCognito(cognitoSubject)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tutor was not found for authenticated user"));
        return tutorSchoolRepository.findActiveSchoolsByTutorId(tutor.getId());
    }
}
