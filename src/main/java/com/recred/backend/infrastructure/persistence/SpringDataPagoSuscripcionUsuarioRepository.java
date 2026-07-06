package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.PagoSuscripcionUsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataPagoSuscripcionUsuarioRepository
        extends JpaRepository<PagoSuscripcionUsuarioEntity, UUID> {

    Optional<PagoSuscripcionUsuarioEntity> findByMercadoPagoPaymentId(String paymentId);
}
