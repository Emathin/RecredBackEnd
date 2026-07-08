package com.recred.backend.application;

import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.services.ExternalPaymentService;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

public class CreateSchoolSubscriptionPayment {

    private final ColegioRepository schoolRepository;
    private final ExternalPaymentService externalPaymentService;
    private final SchoolSubscriptionPriceCatalog priceCatalog;

    public CreateSchoolSubscriptionPayment(
            ColegioRepository schoolRepository,
            ExternalPaymentService externalPaymentService,
            SchoolSubscriptionPriceCatalog priceCatalog
    ) {
        this.schoolRepository = schoolRepository;
        this.externalPaymentService = externalPaymentService;
        this.priceCatalog = priceCatalog;
    }

    @Transactional(readOnly = true)
    public CreateSchoolSubscriptionPaymentResponse execute(
            CreateSchoolSubscriptionPaymentRequest paymentRequest
    ) {
        ensureSchoolExistsAndIsActive(paymentRequest.colegioId());
        BigDecimal price = priceCatalog.monthlyPrice();
        String paymentUrl = paymentPreferenceUrlFor(paymentRequest, price);
        return responseFrom(paymentUrl, price);
    }

    private void ensureSchoolExistsAndIsActive(UUID schoolId) {
        if (!schoolRepository.existsActiveById(schoolId)) {
            throw new SchoolNotFoundException(schoolId);
        }
    }

    private String paymentPreferenceUrlFor(
            CreateSchoolSubscriptionPaymentRequest paymentRequest,
            BigDecimal price
    ) {
        return externalPaymentService.createSchoolSubscriptionPreference(
                paymentRequest.colegioId(),
                price
        );
    }

    private CreateSchoolSubscriptionPaymentResponse responseFrom(
            String paymentUrl,
            BigDecimal price
    ) {
        return new CreateSchoolSubscriptionPaymentResponse(
                paymentUrl,
                price,
                priceCatalog.currency()
        );
    }
}
