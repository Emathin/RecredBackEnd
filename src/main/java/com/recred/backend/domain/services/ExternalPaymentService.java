package com.recred.backend.domain.services;

import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.TipoPlan;

import java.math.BigDecimal;
import java.util.UUID;

public interface ExternalPaymentService {

    String createTopUpPreference(UUID studentId, BigDecimal amount);

    String createScheduledTopUpPreapproval(
            UUID studentId, BigDecimal amount, Integer frequency, String frequencyType,
            String payerEmail);

    String createUserSubscriptionPreference(
            UUID usuarioId, TipoPlan plan, PeriodoSuscripcion periodo, BigDecimal amount);

    String createSchoolSubscriptionPreference(UUID colegioId, BigDecimal amount);

    boolean isValidSignature(String signature, String requestId, String paymentId);

    com.recred.backend.domain.models.PaymentDetails getPaymentDetails(String paymentId);
}
