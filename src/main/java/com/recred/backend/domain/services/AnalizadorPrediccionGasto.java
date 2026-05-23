package com.recred.backend.domain.services;

import com.recred.backend.domain.models.AnalisisPrediccionGasto;
import com.recred.backend.domain.models.PrediccionGasto;
import java.util.Optional;

public interface AnalizadorPrediccionGasto {
    Optional<AnalisisPrediccionGasto> analizar(PrediccionGasto prediccionGasto);
}
