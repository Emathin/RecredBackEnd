package com.recred.backend.domain.services;

import com.recred.backend.domain.models.PaymentDetails;
import java.math.BigDecimal;
import java.util.UUID;

public interface ExternalPaymentService {

    String createTopUpPreference(UUID studentId, BigDecimal amount);

    boolean isValidSignature(String signature, String requestId, String paymentId);

    PaymentDetails getPaymentDetails(String paymentId);
}
