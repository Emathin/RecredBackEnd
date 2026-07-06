package com.recred.backend.domain.models;

public enum EstadoPagoExterno {
    PENDIENTE,
    APROBADO,
    RECHAZADO,
    CANCELADO,
    REEMBOLSADO,
    EN_PROCESO;

    public static EstadoPagoExterno from(PaymentStatus status) {
        return switch (status) {
            case APPROVED -> APROBADO;
            case REJECTED -> RECHAZADO;
            case CANCELLED -> CANCELADO;
            case REFUNDED, CHARGEBACK -> REEMBOLSADO;
            case PENDING -> PENDIENTE;
        };
    }
}
