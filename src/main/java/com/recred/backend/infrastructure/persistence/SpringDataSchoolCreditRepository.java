package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.infrastructure.entities.SchoolCreditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataSchoolCreditRepository extends JpaRepository<SchoolCreditEntity, UUID> {
    Optional<SchoolCreditEntity> findByAlumnoIdAndStatus(UUID alumnoId, CreditStatus status);
}
