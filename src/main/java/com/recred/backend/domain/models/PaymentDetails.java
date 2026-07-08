package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentDetails(
    String paymentId,
    UUID studentId,
    BigDecimal amount,
    PaymentStatus status,
    PaymentPurpose purpose,
    UUID usuarioId,
    TipoPlan plan,
    PeriodoSuscripcion periodo,
    UUID colegioId
) {
    public PaymentDetails(
            String paymentId,
            UUID studentId,
            BigDecimal amount,
            PaymentStatus status
    ) {
        this(paymentId, studentId, amount, status, PaymentPurpose.TOP_UP, null, null, null, null);
    }

    public PaymentDetails(
            String paymentId,
            UUID studentId,
            BigDecimal amount,
            PaymentStatus status,
            PaymentPurpose purpose,
            UUID usuarioId,
            TipoPlan plan,
            PeriodoSuscripcion periodo
    ) {
        this(paymentId, studentId, amount, status, purpose, usuarioId, plan, periodo, null);
    }

    public static PaymentDetails fromReference(
            String paymentId,
            PaymentReference reference,
            BigDecimal amount,
            PaymentStatus status
    ) {
        if (reference.isTopUp()) {
            return new PaymentDetails(paymentId, reference.targetId(), amount, status);
        }
        if (reference.isSchoolSubscription()) {
            return new PaymentDetails(
                    paymentId,
                    null,
                    amount,
                    status,
                    PaymentPurpose.SCHOOL_SUBSCRIPTION,
                    null,
                    null,
                    null,
                    reference.targetId()
            );
        }

        return new PaymentDetails(
                paymentId,
                null,
                amount,
                status,
                PaymentPurpose.USER_SUBSCRIPTION,
                reference.targetId(),
                reference.plan(),
                reference.periodo(),
                null
        );
    }

    public boolean wasApproved() {
        return status == PaymentStatus.APPROVED;
    }

    public boolean isApprovedTopUp() {
        return isTopUp() && wasApproved();
    }

    public boolean isTopUp() {
        return purpose == PaymentPurpose.TOP_UP;
    }

    public boolean isUserSubscription() {
        return purpose == PaymentPurpose.USER_SUBSCRIPTION;
    }

    public boolean isSchoolSubscription() {
        return purpose == PaymentPurpose.SCHOOL_SUBSCRIPTION;
    }
}
