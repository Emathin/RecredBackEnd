package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.TutorSchool;
import com.recred.backend.domain.models.TutorSchoolStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TutorSchoolRepository {

    Optional<TutorSchool> findByTutorIdAndSchoolId(UUID tutorId, UUID schoolId);

    boolean existsByTutorIdAndSchoolIdAndStatus(
            UUID tutorId,
            UUID schoolId,
            TutorSchoolStatus status);

    List<Colegio> findActiveSchoolsByTutorId(UUID tutorId);

    TutorSchool save(TutorSchool tutorSchool);
}
