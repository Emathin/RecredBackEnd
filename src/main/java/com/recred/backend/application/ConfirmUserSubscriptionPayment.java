package com.recred.backend.application;

import com.recred.backend.domain.exceptions.UsuarioNoEncontradoException;
import com.recred.backend.domain.models.PagoSuscripcionUsuario;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.repositories.PagoSuscripcionUsuarioRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

public class ConfirmUserSubscriptionPayment {

    private final PagoSuscripcionUsuarioRepository paymentRepository;
    private final UsuarioRepository usuarioRepository;
    private final ActivateUserSubscription activateUserSubscription;
    private final Clock clock;

    public ConfirmUserSubscriptionPayment(
            PagoSuscripcionUsuarioRepository paymentRepository,
            UsuarioRepository usuarioRepository,
            ActivateUserSubscription activateUserSubscription,
            Clock clock
    ) {
        this.paymentRepository = paymentRepository;
        this.usuarioRepository = usuarioRepository;
        this.activateUserSubscription = activateUserSubscription;
        this.clock = clock;
    }

    @Transactional
    public ConfirmUserSubscriptionPaymentResponse execute(
            ConfirmUserSubscriptionPaymentRequest confirmationRequest
    ) {
        PaymentDetails paymentDetails = confirmationRequest.paymentDetails();
        lockSubscriptionOwner(paymentDetails);
        PagoSuscripcionUsuario payment = paymentFrom(paymentDetails);
        if (payment.alreadyActivatedSubscription()) {
            return subscriptionNotActivated();
        }

        PagoSuscripcionUsuario savedPayment = saveUpdatedPayment(payment, paymentDetails);
        return activateSubscriptionIfNeeded(savedPayment, paymentDetails);
    }

    private void lockSubscriptionOwner(PaymentDetails paymentDetails) {
        usuarioRepository.findByIdForUpdate(paymentDetails.usuarioId())
                .orElseThrow(() -> new UsuarioNoEncontradoException(
                        paymentDetails.usuarioId()));
    }

    private PagoSuscripcionUsuario paymentFrom(PaymentDetails paymentDetails) {
        return paymentRepository.findByMercadoPagoPaymentId(paymentDetails.paymentId())
                .orElseGet(() -> PagoSuscripcionUsuario.fromPaymentDetails(paymentDetails));
    }

    private void updatePayment(PagoSuscripcionUsuario payment, PaymentDetails paymentDetails) {
        payment.updateFrom(paymentDetails);
    }

    private PagoSuscripcionUsuario saveUpdatedPayment(
            PagoSuscripcionUsuario payment,
            PaymentDetails paymentDetails
    ) {
        updatePayment(payment, paymentDetails);
        return paymentRepository.save(payment);
    }

    private ConfirmUserSubscriptionPaymentResponse activateSubscriptionIfNeeded(
            PagoSuscripcionUsuario payment,
            PaymentDetails paymentDetails
    ) {
        if (!payment.shouldActivateSubscription()) {
            return subscriptionNotActivated();
        }

        activateSubscription(payment, paymentDetails);
        paymentRepository.save(payment);
        return new ConfirmUserSubscriptionPaymentResponse(true);
    }

    private ConfirmUserSubscriptionPaymentResponse subscriptionNotActivated() {
        return new ConfirmUserSubscriptionPaymentResponse(false);
    }

    private void activateSubscription(
            PagoSuscripcionUsuario payment,
            PaymentDetails paymentDetails
    ) {
        ActivateUserSubscriptionResponse subscription = activateUserSubscription.execute(
                new ActivateUserSubscriptionRequest(
                        paymentDetails.usuarioId(),
                        paymentDetails.plan(),
                        paymentDetails.periodo(),
                        paymentDetails.amount(),
                        LocalDateTime.now(clock)
                )
        );
        payment.markSubscriptionActivated(subscription.subscriptionId(), LocalDateTime.now(clock));
    }
}
