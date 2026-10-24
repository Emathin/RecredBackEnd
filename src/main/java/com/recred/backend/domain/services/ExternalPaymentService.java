package com.recred.backend.domain.services;

import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.TipoPlan;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface ExternalPaymentService {

    String createTopUpPreference(UUID studentId, BigDecimal amount);

    default String createScheduledTopUpPreapproval(
            UUID studentId, BigDecimal amount, Integer frequency, String frequencyType,
            String payerEmail) {
        return createScheduledTopUpPreapproval(
                studentId, amount, frequency, frequencyType, payerEmail, null);
    }

    String createScheduledTopUpPreapproval(
            UUID studentId, BigDecimal amount, Integer frequency, String frequencyType,
            String payerEmail, OffsetDateTime startDate);

    String createUserSubscriptionPreference(
            UUID usuarioId, TipoPlan plan, PeriodoSuscripcion periodo, BigDecimal amount);

    String createSchoolSubscriptionPreference(UUID colegioId, BigDecimal amount);

    boolean isValidSignature(String signature, String requestId, String paymentId);

    com.recred.backend.domain.models.PaymentDetails getPaymentDetails(String paymentId);
}
