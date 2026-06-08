package com.recred.backend.domain.services;

import com.recred.backend.domain.models.PaymentDetails;
import java.math.BigDecimal;
import java.util.UUID;

public interface ExternalPaymentService {

    // Contrato para generar el link de pago (preferencia)
    String createTopUpPreference(UUID studentId, BigDecimal amount);

    // Valida la autenticidad del webhook usando HMAC-SHA256
    boolean isValidSignature(String signature, String requestId, String paymentId);

    // Recupera los detalles verificados del pago desde la API oficial
    PaymentDetails getPaymentDetails(String paymentId);
}
