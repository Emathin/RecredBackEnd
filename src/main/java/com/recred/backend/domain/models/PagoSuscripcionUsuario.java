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
public class PagoSuscripcionUsuario {

    private static final String DEFAULT_CURRENCY = "ARS";

    private UUID id;
    private String mercadoPagoPaymentId;
    private UUID usuarioId;
    private UUID suscripcionId;
    private TipoPlan plan;
    private PeriodoSuscripcion periodo;
    private BigDecimal monto;
    private String moneda = DEFAULT_CURRENCY;
    private EstadoPagoExterno estado;
    private LocalDateTime creadoEn;
    private LocalDateTime aprobadoEn;

    public static PagoSuscripcionUsuario fromPaymentDetails(PaymentDetails details) {
        PagoSuscripcionUsuario payment = new PagoSuscripcionUsuario();
        payment.setMercadoPagoPaymentId(details.paymentId());
        payment.setUsuarioId(details.usuarioId());
        payment.setPlan(details.plan());
        payment.setPeriodo(details.periodo());
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
