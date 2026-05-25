package com.recred.backend.domain.repositories;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface CompraRepository {

    BigDecimal sumarGastosPorAlumnoEnRango(UUID alumnoId,
                                           LocalDateTime inicio, LocalDateTime fin);

    BigDecimal sumarGastosPorCategoriaEnRango(UUID alumnoId,
                                              UUID categoriaId, LocalDateTime inicio, LocalDateTime fin);
}