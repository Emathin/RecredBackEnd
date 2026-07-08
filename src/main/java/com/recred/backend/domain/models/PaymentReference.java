package com.recred.backend.domain.models;

import java.util.UUID;

public record PaymentReference(
        PaymentPurpose purpose,
        UUID targetId,
        TipoPlan plan,
        PeriodoSuscripcion periodo
) {

    private static final String SEPARATOR = "|";

    public static PaymentReference topUp(UUID studentId) {
        return new PaymentReference(PaymentPurpose.TOP_UP, studentId, null, null);
    }

    public static PaymentReference userSubscription(
            UUID usuarioId,
            TipoPlan plan,
            PeriodoSuscripcion periodo
    ) {
        return new PaymentReference(PaymentPurpose.USER_SUBSCRIPTION, usuarioId, plan, periodo);
    }

    public static PaymentReference schoolSubscription(UUID colegioId) {
        return new PaymentReference(PaymentPurpose.SCHOOL_SUBSCRIPTION, colegioId, null, null);
    }

    public static PaymentReference fromExternalReference(String externalReference) {
        if (isLegacyTopUpReference(externalReference)) {
            return topUp(UUID.fromString(externalReference));
        }

        String[] parts = externalReference.split("\\|");
        return parseTypedReference(parts);
    }

    public String toExternalReference() {
        if (purpose == PaymentPurpose.TOP_UP) {
            return purpose.name() + SEPARATOR + targetId;
        }

        if (purpose == PaymentPurpose.SCHOOL_SUBSCRIPTION) {
            return purpose.name() + SEPARATOR + targetId;
        }

        return purpose.name() + SEPARATOR + targetId + SEPARATOR + plan + SEPARATOR + periodo;
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

    private static PaymentReference parseTypedReference(String[] parts) {
        PaymentPurpose purpose = PaymentPurpose.valueOf(parts[0]);
        if (purpose == PaymentPurpose.TOP_UP) {
            return topUp(UUID.fromString(parts[1]));
        }
        if (purpose == PaymentPurpose.SCHOOL_SUBSCRIPTION) {
            return schoolSubscription(UUID.fromString(parts[1]));
        }

        return userSubscription(
                UUID.fromString(parts[1]),
                TipoPlan.valueOf(parts[2]),
                PeriodoSuscripcion.valueOf(parts[3])
        );
    }

    private static boolean isLegacyTopUpReference(String externalReference) {
        return externalReference != null && !externalReference.contains(SEPARATOR);
    }
}
