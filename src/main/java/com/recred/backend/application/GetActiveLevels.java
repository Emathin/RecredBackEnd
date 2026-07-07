package com.recred.backend.application;

import com.recred.backend.domain.repositories.NivelRepository;

public class GetActiveLevels {

    private final NivelRepository repository;

    public GetActiveLevels(NivelRepository repository) {
        this.repository = repository;
    }

    public GetActiveLevelsResponse execute(GetActiveLevelsRequest request) {
        return GetActiveLevelsResponse.fromDomain(repository.findAllActive());
    }
}
