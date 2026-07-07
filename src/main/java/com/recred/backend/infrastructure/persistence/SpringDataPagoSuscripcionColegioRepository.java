package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.PagoSuscripcionColegioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataPagoSuscripcionColegioRepository
        extends JpaRepository<PagoSuscripcionColegioEntity, UUID> {

    Optional<PagoSuscripcionColegioEntity> findByMercadoPagoPaymentId(String paymentId);
}
