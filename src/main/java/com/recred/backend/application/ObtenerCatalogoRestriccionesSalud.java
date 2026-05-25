package com.recred.backend.application;

import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;

import java.util.List;

public class ObtenerCatalogoRestriccionesSalud {

    private final ClasificacionSaludRepository repository;

    public ObtenerCatalogoRestriccionesSalud(ClasificacionSaludRepository repository) {
        this.repository = repository;
    }

    public List<ClasificacionSalud> ejecutar() {
        return repository.findAll();
    }
}