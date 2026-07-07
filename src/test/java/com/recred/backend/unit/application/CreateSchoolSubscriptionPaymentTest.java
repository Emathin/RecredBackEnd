package com.recred.backend.unit.application;

import com.recred.backend.application.CreateSchoolSubscriptionPayment;
import com.recred.backend.application.CreateSchoolSubscriptionPaymentRequest;
import com.recred.backend.application.CreateSchoolSubscriptionPaymentResponse;
import com.recred.backend.application.SchoolSubscriptionPriceCatalog;
import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.services.ExternalPaymentService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateSchoolSubscriptionPaymentTest {

    private final ColegioRepository colegioRepository = mock(ColegioRepository.class);
    private final ExternalPaymentService externalPaymentService =
            mock(ExternalPaymentService.class);
    private final CreateSchoolSubscriptionPayment useCase = new CreateSchoolSubscriptionPayment(
            colegioRepository,
            externalPaymentService,
            new SchoolSubscriptionPriceCatalog()
    );

    @Test
    void execute_WhenSchoolExists_ShouldCreatePreferenceWithInstitutionalPrice() {
        UUID schoolId = UUID.randomUUID();
        givenActiveSchool(schoolId);
        givenPaymentUrl(schoolId);

        CreateSchoolSubscriptionPaymentResponse result = whenExecuting(schoolId);

        assertEquals("https://pay.test/school", result.paymentUrl());
        assertEquals(new BigDecimal("20.00"), result.price());
        assertEquals("USD", result.currency());
        verify(externalPaymentService).createSchoolSubscriptionPreference(
                schoolId,
                new BigDecimal("20.00")
        );
    }

    @Test
    void execute_WhenSchoolDoesNotExist_ShouldThrowException() {
        UUID schoolId = UUID.randomUUID();
        givenMissingSchool(schoolId);

        assertThrows(SchoolNotFoundException.class, () -> whenExecuting(schoolId));
        verify(externalPaymentService, never()).createSchoolSubscriptionPreference(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private void givenActiveSchool(UUID schoolId) {
        when(colegioRepository.existsActiveById(schoolId)).thenReturn(true);
    }

    private void givenMissingSchool(UUID schoolId) {
        when(colegioRepository.existsActiveById(schoolId)).thenReturn(false);
    }

    private void givenPaymentUrl(UUID schoolId) {
        when(externalPaymentService.createSchoolSubscriptionPreference(
                schoolId,
                new BigDecimal("20.00")
        )).thenReturn("https://pay.test/school");
    }

    private CreateSchoolSubscriptionPaymentResponse whenExecuting(UUID schoolId) {
        return useCase.execute(new CreateSchoolSubscriptionPaymentRequest(schoolId));
    }
}
