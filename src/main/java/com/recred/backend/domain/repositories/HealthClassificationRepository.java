package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.HealthClassification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthClassificationRepository {
    List<HealthClassification> findAllById(List<UUID> ids);
    Optional<HealthClassification> findById(UUID id);
    List<HealthClassification> findAll();
}