package com.recred.backend.application;

import com.recred.backend.domain.models.PagoSuscripcionColegio;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.repositories.PagoSuscripcionColegioRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

public class ConfirmSchoolSubscriptionPayment {

    private static final String CURRENCY = "USD";

    private final PagoSuscripcionColegioRepository paymentRepository;
    private final ActivateSchoolSubscription activateSchoolSubscription;
    private final Clock clock;

    public ConfirmSchoolSubscriptionPayment(
            PagoSuscripcionColegioRepository paymentRepository,
            ActivateSchoolSubscription activateSchoolSubscription,
            Clock clock
    ) {
        this.paymentRepository = paymentRepository;
        this.activateSchoolSubscription = activateSchoolSubscription;
        this.clock = clock;
    }

    @Transactional
    public ConfirmSchoolSubscriptionPaymentResponse execute(
            ConfirmSchoolSubscriptionPaymentRequest request
    ) {
        PagoSuscripcionColegio payment = payment(request.paymentDetails());
        if (payment.alreadyActivatedSubscription()) {
            return new ConfirmSchoolSubscriptionPaymentResponse(false);
        }

        updatePayment(payment, request.paymentDetails());
        activateSubscriptionIfApproved(payment, request.paymentDetails());
        paymentRepository.save(payment);
        return new ConfirmSchoolSubscriptionPaymentResponse(payment.getSuscripcionId() != null);
    }

    private PagoSuscripcionColegio payment(PaymentDetails details) {
        return paymentRepository.findByMercadoPagoPaymentId(details.paymentId())
                .orElseGet(() -> PagoSuscripcionColegio.fromPaymentDetails(details));
    }

    private void updatePayment(PagoSuscripcionColegio payment, PaymentDetails details) {
        payment.updateFrom(details);
    }

    private void activateSubscriptionIfApproved(
            PagoSuscripcionColegio payment,
            PaymentDetails details
    ) {
        if (!payment.shouldActivateSubscription()) {
            return;
        }

        ActivateSchoolSubscriptionResponse subscription = activateSchoolSubscription.execute(
                new ActivateSchoolSubscriptionRequest(
                        details.colegioId(),
                        details.amount(),
                        CURRENCY,
                        LocalDateTime.now(clock)
                )
        );
        payment.markSubscriptionActivated(subscription.subscriptionId(), LocalDateTime.now(clock));
    }
}
