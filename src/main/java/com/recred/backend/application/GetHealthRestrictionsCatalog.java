package com.recred.backend.application;

import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;

import java.util.List;

public class GetHealthRestrictionsCatalog {

    private final ClasificacionSaludRepository repository;

    public GetHealthRestrictionsCatalog(ClasificacionSaludRepository repository) {
        this.repository = repository;
    }

    public List<ClasificacionSalud> execute() {
        return repository.findAll();
    }
}
