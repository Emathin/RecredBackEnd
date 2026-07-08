package com.recred.backend.application;

import com.recred.backend.domain.exceptions.UsuarioNoEncontradoException;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.ExternalPaymentService;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

public class CreateUserSubscriptionPayment {

    private static final String CURRENCY = "ARS";

    private final UsuarioRepository userRepository;
    private final ExternalPaymentService externalPaymentService;
    private final SubscriptionPriceCatalog priceCatalog;

    public CreateUserSubscriptionPayment(
            UsuarioRepository userRepository,
            ExternalPaymentService externalPaymentService,
            SubscriptionPriceCatalog priceCatalog
    ) {
        this.userRepository = userRepository;
        this.externalPaymentService = externalPaymentService;
        this.priceCatalog = priceCatalog;
    }

    @Transactional(readOnly = true)
    public CreateUserSubscriptionPaymentResponse execute(
            CreateUserSubscriptionPaymentRequest paymentRequest
    ) {
        Usuario user = findUserAllowedToSubscribe(paymentRequest);
        BigDecimal price = priceCatalog.priceFor(paymentRequest.plan(), paymentRequest.periodo());
        String paymentUrl = paymentPreferenceUrlFor(user, paymentRequest, price);
        return responseFrom(paymentRequest, price, paymentUrl);
    }

    private Usuario findUserAllowedToSubscribe(
            CreateUserSubscriptionPaymentRequest paymentRequest
    ) {
        Usuario user = userRepository.findById(paymentRequest.usuarioId())
                .orElseThrow(() -> new UsuarioNoEncontradoException(
                        paymentRequest.usuarioId()));
        user.validateCanHaveIndividualSubscription();
        return user;
    }

    private String paymentPreferenceUrlFor(
            Usuario user,
            CreateUserSubscriptionPaymentRequest paymentRequest,
            BigDecimal price
    ) {
        return externalPaymentService.createUserSubscriptionPreference(
                user.getId(),
                paymentRequest.plan(),
                paymentRequest.periodo(),
                price
        );
    }

    private CreateUserSubscriptionPaymentResponse responseFrom(
            CreateUserSubscriptionPaymentRequest paymentRequest,
            BigDecimal price,
            String paymentUrl
    ) {
        return new CreateUserSubscriptionPaymentResponse(
                paymentUrl,
                paymentRequest.plan(),
                paymentRequest.periodo(),
                price,
                CURRENCY
        );
    }
}
