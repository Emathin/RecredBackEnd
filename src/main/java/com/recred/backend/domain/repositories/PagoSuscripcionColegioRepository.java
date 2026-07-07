package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.PagoSuscripcionColegio;

import java.util.Optional;

public interface PagoSuscripcionColegioRepository {
    Optional<PagoSuscripcionColegio> findByMercadoPagoPaymentId(String paymentId);

    PagoSuscripcionColegio save(PagoSuscripcionColegio payment);
}
