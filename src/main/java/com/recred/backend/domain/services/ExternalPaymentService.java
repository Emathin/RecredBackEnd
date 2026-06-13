package com.recred.backend.domain.services;

import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.Purchase;
import java.math.BigDecimal;
import java.util.UUID;

public interface ExternalPaymentService {

    String createTopUpPreference(UUID studentId, BigDecimal amount);

    String createPurchasePreference(Purchase purchase);

    boolean isValidSignature(String signature, String requestId, String paymentId);

    PaymentDetails getPaymentDetails(String paymentId);
}
