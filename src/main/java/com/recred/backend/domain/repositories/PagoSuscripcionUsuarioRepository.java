package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.PagoSuscripcionUsuario;

import java.util.Optional;

public interface PagoSuscripcionUsuarioRepository {
    Optional<PagoSuscripcionUsuario> findByMercadoPagoPaymentId(String paymentId);

    PagoSuscripcionUsuario save(PagoSuscripcionUsuario payment);
}
