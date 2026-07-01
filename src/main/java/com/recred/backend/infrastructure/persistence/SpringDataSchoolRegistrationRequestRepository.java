package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.SchoolRegistrationStatus;
import com.recred.backend.infrastructure.entities.SchoolRegistrationRequestEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataSchoolRegistrationRequestRepository 
        extends JpaRepository<SchoolRegistrationRequestEntity, UUID> {
        
    @EntityGraph(attributePaths = {"schoolLevel"})
    List<SchoolRegistrationRequestEntity> findByStatusOrderByCreatedAtDesc(
            SchoolRegistrationStatus status);

    @EntityGraph(attributePaths = {"schoolLevel"})
    Optional<SchoolRegistrationRequestEntity> findById(UUID id);
    
    boolean existsBySchoolEmailAndStatus(String schoolEmail, SchoolRegistrationStatus status);
    boolean existsBySchoolCueAndStatus(String schoolCue, SchoolRegistrationStatus status);
    boolean existsByDirectorEmailAndStatus(String directorEmail, SchoolRegistrationStatus status);
}
