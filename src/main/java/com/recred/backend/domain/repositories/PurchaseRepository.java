package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.CartProductAffinity;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseResume;

import com.recred.backend.domain.models.EstadoRetiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PurchaseRepository {

    List<Purchase> findHistorialByAlumno(UUID alumnoId);
    List<Purchase> findByAlumnoAndDateRange(UUID alumnoId, LocalDateTime start, LocalDateTime end);
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

    List<Purchase> findKiosqueroOrders(UUID buffetId,
                                       List<EstadoCompra> statuses,
                                       LocalDate date,
                                       UUID pickupSlotId,
                                       int limit);

    List<Purchase> findByBuffetAndDateRange(UUID buffetId,
                                            LocalDateTime start,
                                            LocalDateTime end);

    List<Purchase> findExpiredAdvancePurchases(LocalDate date,
                                               LocalTime time,
                                               List<EstadoCompra> statuses);

    List<Purchase> findExpiredAdvancePurchases(UUID buffetId,
                                               LocalDate date,
                                               LocalTime time,
                                               List<EstadoCompra> statuses);

    long countScheduledAdvancePurchases(UUID buffetId,
                                        UUID pickupSlotId,
                                        LocalDate pickupDate,
                                        List<EstadoCompra> statuses);

    long sumScheduledProductQuantity(UUID buffetId,
                                     UUID productId,
                                     LocalDate pickupDate,
                                     List<EstadoCompra> statuses);

    List<CartProductAffinity> findStudentCartAffinities(
            UUID alumnoId,
            UUID buffetId,
            Set<UUID> cartProductIds);

    List<CartProductAffinity> findBuffetCartAffinities(
            UUID buffetId,
            Set<UUID> cartProductIds);

    List<Purchase> findScheduledPickups(
            UUID buffetId,
            LocalDate fecha,
            EstadoRetiro estadoRetiro,
            UUID franjaId,
            String search);

    long countHealthyPurchasesByAlumno(UUID alumnoId, LocalDateTime since);

    List<Purchase> findPendientesByAlumno(UUID alumnoId);
}
