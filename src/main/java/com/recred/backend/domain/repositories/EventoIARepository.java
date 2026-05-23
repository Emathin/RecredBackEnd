package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.PrediccionGastoResultado;

public interface EventoIARepository {
    void registrarPrediccion(PrediccionGastoResultado resultado);
}
