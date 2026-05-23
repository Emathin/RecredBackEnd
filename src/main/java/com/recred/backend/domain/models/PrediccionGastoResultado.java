package com.recred.backend.domain.models;

import java.util.Optional;

public record PrediccionGastoResultado(
        PrediccionGasto prediccionGasto,
        Optional<AnalisisPrediccionGasto> analisisIa
) {
}
