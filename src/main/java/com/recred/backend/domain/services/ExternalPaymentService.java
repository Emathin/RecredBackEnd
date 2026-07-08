package com.recred.backend.domain.services;

import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.TipoPlan;
import java.math.BigDecimal;
import java.util.UUID;

public interface ExternalPaymentService {

    String createTopUpPreference(UUID studentId, BigDecimal amount);

    String createUserSubscriptionPreference(
            UUID usuarioId,
            TipoPlan plan,
            PeriodoSuscripcion periodo,
            BigDecimal amount
    );

    String createSchoolSubscriptionPreference(UUID colegioId, BigDecimal amount);

    boolean isValidSignature(String signature, String requestId, String paymentId);

    PaymentDetails getPaymentDetails(String paymentId);
}
