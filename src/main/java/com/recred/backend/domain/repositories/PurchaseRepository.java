package com.recred.backend.domain.repositories;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseResume;

import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PurchaseRepository {

    List<Purchase> findHistorialByAlumno(UUID alumnoId);
    List<Purchase> findHistorialByTutor(UUID tutorId);

    Purchase save(Purchase purchase);

    Optional<Purchase> findById(UUID id);

    BigDecimal sumarGastosPorAlumnoEnRango(UUID alumnoId,
                                           LocalDateTime inicio, LocalDateTime fin);

    BigDecimal sumarGastosPorCategoriaEnRango(UUID alumnoId,
                                              UUID categoriaId, LocalDateTime inicio,
                                              LocalDateTime fin);

    List<PreferenceAIRepository.CompraResumen> findLastPurchasesByAlumno(
            UUID alumnoId, int limit);

    List<PurchaseResume> findPurchasesByAlumno(UUID alumnoId);

    List<Purchase> findRecentByBuffet(UUID buffetId, int limit);

    List<Purchase> findByBuffetAndStatuses(UUID buffetId,
                                           List<EstadoCompra> statuses,
                                           int limit);
}
