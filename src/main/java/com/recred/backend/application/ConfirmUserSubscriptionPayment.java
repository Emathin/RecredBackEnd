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
            ConfirmUserSubscriptionPaymentRequest request
    ) {
        PaymentDetails details = request.paymentDetails();
        lockSubscriptionOwner(details);
        PagoSuscripcionUsuario payment = payment(details);
        if (payment.alreadyActivatedSubscription()) {
            return subscriptionNotActivated();
        }

        PagoSuscripcionUsuario savedPayment = saveUpdatedPayment(payment, details);
        return activateSubscriptionIfNeeded(savedPayment, details);
    }

    private void lockSubscriptionOwner(PaymentDetails details) {
        usuarioRepository.findByIdForUpdate(details.usuarioId())
                .orElseThrow(() -> new UsuarioNoEncontradoException(details.usuarioId()));
    }

    private PagoSuscripcionUsuario payment(PaymentDetails details) {
        return paymentRepository.findByMercadoPagoPaymentId(details.paymentId())
                .orElseGet(() -> PagoSuscripcionUsuario.fromPaymentDetails(details));
    }

    private void updatePayment(PagoSuscripcionUsuario payment, PaymentDetails details) {
        payment.updateFrom(details);
    }

    private PagoSuscripcionUsuario saveUpdatedPayment(
            PagoSuscripcionUsuario payment,
            PaymentDetails details
    ) {
        updatePayment(payment, details);
        return paymentRepository.save(payment);
    }

    private ConfirmUserSubscriptionPaymentResponse activateSubscriptionIfNeeded(
            PagoSuscripcionUsuario payment,
            PaymentDetails details
    ) {
        if (!payment.shouldActivateSubscription()) {
            return subscriptionNotActivated();
        }

        activateSubscription(payment, details);
        paymentRepository.save(payment);
        return new ConfirmUserSubscriptionPaymentResponse(true);
    }

    private ConfirmUserSubscriptionPaymentResponse subscriptionNotActivated() {
        return new ConfirmUserSubscriptionPaymentResponse(false);
    }

    private void activateSubscription(
            PagoSuscripcionUsuario payment,
            PaymentDetails details
    ) {
        ActivateUserSubscriptionResponse subscription = activateUserSubscription.execute(
                new ActivateUserSubscriptionRequest(
                        details.usuarioId(),
                        details.plan(),
                        details.periodo(),
                        details.amount(),
                        LocalDateTime.now(clock)
                )
        );
        payment.markSubscriptionActivated(subscription.subscriptionId(), LocalDateTime.now(clock));
    }
}
