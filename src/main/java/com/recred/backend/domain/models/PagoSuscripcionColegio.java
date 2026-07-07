package com.recred.backend.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class PagoSuscripcionColegio {

    private static final String DEFAULT_CURRENCY = "USD";

    private UUID id;
    private String mercadoPagoPaymentId;
    private UUID colegioId;
    private UUID suscripcionId;
    private BigDecimal monto;
    private String moneda = DEFAULT_CURRENCY;
    private EstadoPagoExterno estado;
    private LocalDateTime creadoEn;
    private LocalDateTime aprobadoEn;

    public static PagoSuscripcionColegio fromPaymentDetails(PaymentDetails details) {
        PagoSuscripcionColegio payment = new PagoSuscripcionColegio();
        payment.setMercadoPagoPaymentId(details.paymentId());
        payment.setColegioId(details.colegioId());
        payment.updateFrom(details);
        return payment;
    }

    public void updateFrom(PaymentDetails details) {
        this.monto = details.amount();
        this.estado = EstadoPagoExterno.from(details.status());
    }

    public boolean alreadyActivatedSubscription() {
        return estado == EstadoPagoExterno.APROBADO && suscripcionId != null;
    }

    public boolean shouldActivateSubscription() {
        return estado == EstadoPagoExterno.APROBADO && suscripcionId == null;
    }

    public void markSubscriptionActivated(UUID subscriptionId, LocalDateTime approvedAt) {
        this.suscripcionId = subscriptionId;
        this.aprobadoEn = approvedAt;
        this.estado = EstadoPagoExterno.APROBADO;
    }
}
