package com.recred.backend.unit.application;

import com.recred.backend.application.CreateSchoolSubscriptionPayment;
import com.recred.backend.application.CreateSchoolSubscriptionPaymentRequest;
import com.recred.backend.application.CreateSchoolSubscriptionPaymentResponse;
import com.recred.backend.application.SchoolSubscriptionPriceCatalog;
import com.recred.backend.domain.exceptions.SchoolNotFoundException;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.services.ExternalPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateSchoolSubscriptionPaymentTest {

    @Mock
    private ColegioRepository schoolRepository;

    @Mock
    private ExternalPaymentService externalPaymentService;

    private CreateSchoolSubscriptionPayment useCase;

    private UUID schoolId;
    private CreateSchoolSubscriptionPaymentResponse response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new CreateSchoolSubscriptionPayment(
                schoolRepository,
                externalPaymentService,
                new SchoolSubscriptionPriceCatalog()
        );
    }

    @Test
    void givenSchoolExists_whenCreatingSubscriptionPayment_thenCreatesInstitutionalPreference() {
        givenSchoolPaymentScenario();
        givenActiveSchool();
        givenPaymentPreferenceUrl();

        whenCreatingSchoolSubscriptionPayment();

        thenPaymentPreferenceIsReturned();
        thenInstitutionalPriceIsReturned();
        thenPaymentPreferenceWasCreated();
    }

    @Test
    void givenSchoolDoesNotExist_whenCreatingSubscriptionPayment_thenFailsBeforeCreatingPayment() {
        givenSchoolPaymentScenario();
        givenSchoolDoesNotExist();

        whenCreatingSchoolSubscriptionPaymentFails();

        thenSchoolWasNotFound();
        thenPaymentPreferenceWasNotCreated();
    }

    private void givenSchoolPaymentScenario() {
        schoolId = UUID.randomUUID();
    }

    private void givenActiveSchool() {
        when(schoolRepository.existsActiveById(schoolId)).thenReturn(true);
    }

    private void givenSchoolDoesNotExist() {
        when(schoolRepository.existsActiveById(schoolId)).thenReturn(false);
    }

    private void givenPaymentPreferenceUrl() {
        when(externalPaymentService.createSchoolSubscriptionPreference(
                schoolId,
                new BigDecimal("20.00")
        )).thenReturn("https://pay.test/school");
    }

    private void whenCreatingSchoolSubscriptionPayment() {
        response = useCase.execute(new CreateSchoolSubscriptionPaymentRequest(schoolId));
    }

    private void whenCreatingSchoolSubscriptionPaymentFails() {
        thrownException = assertThrows(
                RuntimeException.class,
                this::whenCreatingSchoolSubscriptionPayment);
    }

    private void thenPaymentPreferenceIsReturned() {
        assertEquals("https://pay.test/school", response.paymentUrl());
    }

    private void thenInstitutionalPriceIsReturned() {
        assertEquals(new BigDecimal("20.00"), response.price());
        assertEquals("USD", response.currency());
    }

    private void thenPaymentPreferenceWasCreated() {
        verify(externalPaymentService).createSchoolSubscriptionPreference(
                schoolId,
                new BigDecimal("20.00")
        );
    }

    private void thenSchoolWasNotFound() {
        assertTrue(thrownException instanceof SchoolNotFoundException);
    }

    private void thenPaymentPreferenceWasNotCreated() {
        verify(externalPaymentService, never()).createSchoolSubscriptionPreference(
                any(),
                any()
        );
    }
}
