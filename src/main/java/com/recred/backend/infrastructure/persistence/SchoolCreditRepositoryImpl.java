package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.SchoolCredit;
import com.recred.backend.domain.repositories.SchoolCreditRepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.SchoolCreditEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SchoolCreditRepositoryImpl implements SchoolCreditRepository {

    private final SpringDataSchoolCreditRepository repository;
    private final SpringDataAlumnoRepository alumnoRepository;

    public SchoolCreditRepositoryImpl(
            SpringDataSchoolCreditRepository repository,
            SpringDataAlumnoRepository alumnoRepository) {
        this.repository = repository;
        this.alumnoRepository = alumnoRepository;
    }

    @Override
    public Optional<SchoolCredit> findByStudentIdAndStatus(UUID studentId, CreditStatus status) {
        return repository.findByAlumnoIdAndStatus(studentId, status)
                .map(SchoolCreditEntity::toDomain);
    }

    @Override
    public SchoolCredit save(SchoolCredit schoolCredit) {
        SchoolCreditEntity entity = new SchoolCreditEntity();
        entity.setId(schoolCredit.getId());
        entity.setAmount(schoolCredit.getAmount());
        entity.setStatus(schoolCredit.getStatus());
        entity.setCreatedAt(schoolCredit.getCreatedAt());

        if (schoolCredit.getStudentId() != null) {
            AlumnoEntity alumno = alumnoRepository.findById(schoolCredit.getStudentId())
                    .orElseThrow(() -> new IllegalArgumentException("Student not found"));
            entity.setAlumno(alumno);
        }

        return repository.save(entity).toDomain();
    }
}
