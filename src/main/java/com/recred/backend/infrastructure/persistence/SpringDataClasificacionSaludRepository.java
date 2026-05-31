package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.HealthClassificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataClasificacionSaludRepository extends
        JpaRepository<HealthClassificationEntity, UUID> {
}