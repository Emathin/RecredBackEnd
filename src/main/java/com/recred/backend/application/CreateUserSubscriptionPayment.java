package com.recred.backend.application;

import com.recred.backend.domain.exceptions.UsuarioNoEncontradoException;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.ExternalPaymentService;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

public class CreateUserSubscriptionPayment {

    private static final String CURRENCY = "ARS";

    private final UsuarioRepository usuarioRepository;
    private final ExternalPaymentService externalPaymentService;
    private final SubscriptionPriceCatalog priceCatalog;

    public CreateUserSubscriptionPayment(
            UsuarioRepository usuarioRepository,
            ExternalPaymentService externalPaymentService,
            SubscriptionPriceCatalog priceCatalog
    ) {
        this.usuarioRepository = usuarioRepository;
        this.externalPaymentService = externalPaymentService;
        this.priceCatalog = priceCatalog;
    }

    @Transactional(readOnly = true)
    public CreateUserSubscriptionPaymentResponse execute(
            CreateUserSubscriptionPaymentRequest request
    ) {
        Usuario user = subscribableUser(request);
        BigDecimal price = priceCatalog.priceFor(request.plan(), request.periodo());
        String paymentUrl = createPreference(user, request, price);
        return response(request, price, paymentUrl);
    }

    private Usuario subscribableUser(CreateUserSubscriptionPaymentRequest request) {
        Usuario user = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new UsuarioNoEncontradoException(request.usuarioId()));
        user.validateCanHaveIndividualSubscription();
        return user;
    }

    private String createPreference(
            Usuario user,
            CreateUserSubscriptionPaymentRequest request,
            BigDecimal price
    ) {
        return externalPaymentService.createUserSubscriptionPreference(
                user.getId(),
                request.plan(),
                request.periodo(),
                price
        );
    }

    private CreateUserSubscriptionPaymentResponse response(
            CreateUserSubscriptionPaymentRequest request,
            BigDecimal price,
            String paymentUrl
    ) {
        return new CreateUserSubscriptionPaymentResponse(
                paymentUrl,
                request.plan(),
                request.periodo(),
                price,
                CURRENCY
        );
    }
}
