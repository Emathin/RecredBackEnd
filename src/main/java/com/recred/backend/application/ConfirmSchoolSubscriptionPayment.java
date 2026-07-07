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
            ConfirmSchoolSubscriptionPaymentRequest confirmationRequest
    ) {
        PaymentDetails paymentDetails = confirmationRequest.paymentDetails();
        PagoSuscripcionColegio payment = paymentFrom(paymentDetails);
        if (payment.alreadyActivatedSubscription()) {
            return new ConfirmSchoolSubscriptionPaymentResponse(false);
        }

        updatePayment(payment, paymentDetails);
        activateSubscriptionIfApproved(payment, paymentDetails);
        paymentRepository.save(payment);
        return new ConfirmSchoolSubscriptionPaymentResponse(payment.getSuscripcionId() != null);
    }

    private PagoSuscripcionColegio paymentFrom(PaymentDetails paymentDetails) {
        return paymentRepository.findByMercadoPagoPaymentId(paymentDetails.paymentId())
                .orElseGet(() -> PagoSuscripcionColegio.fromPaymentDetails(paymentDetails));
    }

    private void updatePayment(PagoSuscripcionColegio payment, PaymentDetails paymentDetails) {
        payment.updateFrom(paymentDetails);
    }

    private void activateSubscriptionIfApproved(
            PagoSuscripcionColegio payment,
            PaymentDetails paymentDetails
    ) {
        if (!payment.shouldActivateSubscription()) {
            return;
        }

        ActivateSchoolSubscriptionResponse subscription = activateSchoolSubscription.execute(
                new ActivateSchoolSubscriptionRequest(
                        paymentDetails.colegioId(),
                        paymentDetails.amount(),
                        CURRENCY,
                        LocalDateTime.now(clock)
                )
        );
        payment.markSubscriptionActivated(subscription.subscriptionId(), LocalDateTime.now(clock));
    }
}
