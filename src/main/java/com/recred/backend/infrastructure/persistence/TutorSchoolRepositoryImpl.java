package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.TutorSchool;
import com.recred.backend.domain.models.TutorSchoolStatus;
import com.recred.backend.domain.repositories.TutorSchoolRepository;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import com.recred.backend.infrastructure.entities.TutorSchoolEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TutorSchoolRepositoryImpl implements TutorSchoolRepository {

    private final SpringDataTutorSchoolRepository repository;
    private final SpringDataTutorRepository tutorRepository;
    private final SpringDataColegioRepository schoolRepository;

    public TutorSchoolRepositoryImpl(
            SpringDataTutorSchoolRepository repository,
            SpringDataTutorRepository tutorRepository,
            SpringDataColegioRepository schoolRepository) {
        this.repository = repository;
        this.tutorRepository = tutorRepository;
        this.schoolRepository = schoolRepository;
    }

    @Override
    public Optional<TutorSchool> findByTutorIdAndSchoolId(UUID tutorId, UUID schoolId) {
        return repository.findByTutorAndSchool(tutorId, schoolId)
                .map(TutorSchoolEntity::toDomain);
    }

    @Override
    public boolean existsByTutorIdAndSchoolIdAndStatus(
            UUID tutorId,
            UUID schoolId,
            TutorSchoolStatus status) {
        return repository.existsByTutorAndSchoolAndStatus(tutorId, schoolId, status);
    }

    @Override
    public List<Colegio> findActiveSchoolsByTutorId(UUID tutorId) {
        return repository.findByTutorAndStatusOrderBySchoolNameAsc(
                        tutorId,
                        TutorSchoolStatus.ACTIVE)
                .stream()
                .map(TutorSchoolEntity::getSchool)
                .map(ColegioEntity::toDomain)
                .toList();
    }

    @Override
    public TutorSchool save(TutorSchool tutorSchool) {
        TutorSchoolEntity entity = tutorSchool.getId() == null
                ? new TutorSchoolEntity()
                : repository.findById(tutorSchool.getId()).orElseGet(TutorSchoolEntity::new);

        entity.setId(tutorSchool.getId());
        entity.setStatus(tutorSchool.getStatus());

        if (tutorSchool.getTutorId() != null) {
            TutorEntity tutor = tutorRepository.getReferenceById(tutorSchool.getTutorId());
            entity.setTutor(tutor);
        }
        if (tutorSchool.getSchool() != null && tutorSchool.getSchool().getId() != null) {
            ColegioEntity school = schoolRepository.getReferenceById(
                    tutorSchool.getSchool().getId());
            entity.setSchool(school);
        }

        return repository.save(entity).toDomain();
    }
}
