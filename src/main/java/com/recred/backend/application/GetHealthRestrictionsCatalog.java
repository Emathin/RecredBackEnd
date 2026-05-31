package com.recred.backend.application;

import com.recred.backend.domain.models.HealthClassification;
import com.recred.backend.domain.repositories.HealthClassificationRepository;

import java.util.List;

public class GetHealthRestrictionsCatalog {

    private final HealthClassificationRepository repository;

    public GetHealthRestrictionsCatalog(HealthClassificationRepository repository) {
        this.repository = repository;
    }

    public List<HealthClassification> ejecutar() {
        return repository.findAll();
    }
}