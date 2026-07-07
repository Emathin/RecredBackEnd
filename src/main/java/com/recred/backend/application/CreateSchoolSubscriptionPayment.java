package com.recred.backend.application;

import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.services.ExternalPaymentService;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

public class CreateSchoolSubscriptionPayment {

    private final ColegioRepository colegioRepository;
    private final ExternalPaymentService externalPaymentService;
    private final SchoolSubscriptionPriceCatalog priceCatalog;

    public CreateSchoolSubscriptionPayment(
            ColegioRepository colegioRepository,
            ExternalPaymentService externalPaymentService,
            SchoolSubscriptionPriceCatalog priceCatalog
    ) {
        this.colegioRepository = colegioRepository;
        this.externalPaymentService = externalPaymentService;
        this.priceCatalog = priceCatalog;
    }

    @Transactional(readOnly = true)
    public CreateSchoolSubscriptionPaymentResponse execute(
            CreateSchoolSubscriptionPaymentRequest request
    ) {
        requireActiveSchool(request.colegioId());
        BigDecimal price = priceCatalog.monthlyPrice();
        String paymentUrl = externalPaymentService.createSchoolSubscriptionPreference(
                request.colegioId(),
                price
        );
        return new CreateSchoolSubscriptionPaymentResponse(
                paymentUrl,
                price,
                priceCatalog.currency()
        );
    }

    private void requireActiveSchool(UUID colegioId) {
        if (!colegioRepository.existsActiveById(colegioId)) {
            throw new SchoolNotFoundException(colegioId);
        }
    }
}
