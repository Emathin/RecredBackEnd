package com.recred.backend.application;

import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;

import java.util.stream.Collectors;

public class GetHealthRestrictionsCatalog {

    private final ClasificacionSaludRepository repository;

    public GetHealthRestrictionsCatalog(ClasificacionSaludRepository repository) {
        this.repository = repository;
    }

    public GetHealthRestrictionsCatalogResponse execute(
            GetHealthRestrictionsCatalogRequest request
    ) {
        return new GetHealthRestrictionsCatalogResponse(repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    private GetHealthRestrictionsResponse toResponse(ClasificacionSalud clasificacion) {
        return new GetHealthRestrictionsResponse(
                clasificacion.getId(),
                clasificacion.getDescripcion(),
                clasificacion.getActivo()
        );
    }
}
