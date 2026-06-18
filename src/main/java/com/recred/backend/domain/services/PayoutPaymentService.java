package com.recred.backend.domain.services;

import com.recred.backend.domain.models.PaymentDetails;
import java.math.BigDecimal;

public interface PayoutPaymentService {
    PaymentDetails transferToThirdParty(
        String destinationCvu,
        String destinationCuit,
        String accountHolderName,
        BigDecimal amount,
        String idempotencyKey
    );
}
