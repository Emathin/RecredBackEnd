package com.recred.backend.domain.repositories;
import com.recred.backend.domain.models.Purchase;

import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PurchaseRepository {

    void save(Purchase purchase);

    Optional<Purchase> findById(UUID id);

    BigDecimal sumarGastosPorAlumnoEnRango(UUID alumnoId,
                                           LocalDateTime inicio, LocalDateTime fin);

    BigDecimal sumarGastosPorCategoriaEnRango(UUID alumnoId,
                                              UUID categoriaId, LocalDateTime inicio,
                                              LocalDateTime fin);
}
