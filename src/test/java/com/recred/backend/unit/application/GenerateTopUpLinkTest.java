package com.recred.backend.unit.application;

import com.recred.backend.application.GenerateTopUpLink;
import com.recred.backend.application.GenerateTopUpLinkRequest;
import com.recred.backend.application.GenerateTopUpLinkResponse;
import com.recred.backend.domain.repositories.AlumnoRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateTopUpLinkTest {

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private ExternalPaymentService externalPaymentService;

    private GenerateTopUpLink useCase;

    private UUID studentId;
    private BigDecimal amount;
    private String paymentUrl;
    private GenerateTopUpLinkResponse response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new GenerateTopUpLink(alumnoRepository, externalPaymentService);
    }

    @Test
    void givenExistingStudent_whenGeneratingTopUpLink_thenReturnsPaymentUrl() {
        givenTopUpScenario();
        givenStudentExists();
        givenPaymentProviderCreatesTopUpLink();

        whenGeneratingTopUpLink();

        thenPaymentUrlIsReturned();
        thenPaymentProviderWasCalled();
    }

    @Test
    void givenMissingStudent_whenGeneratingTopUpLink_thenFailsAndDoesNotCreatePayment() {
        givenTopUpScenario();
        givenStudentDoesNotExist();

        whenGeneratingTopUpLinkFails();

        thenGenerationFailsWith("Alumno no encontrado");
        thenPaymentProviderWasNotCalled();
    }

    private void givenTopUpScenario() {
        studentId = UUID.randomUUID();
        amount = new BigDecimal("100.00");
        paymentUrl = "https://link.pago";
    }

    private void givenStudentExists() {
        when(alumnoRepository.existsById(studentId)).thenReturn(true);
    }

    private void givenStudentDoesNotExist() {
        when(alumnoRepository.existsById(studentId)).thenReturn(false);
    }

    private void givenPaymentProviderCreatesTopUpLink() {
        when(externalPaymentService.createTopUpPreference(studentId, amount))
                .thenReturn(paymentUrl);
    }

    private void whenGeneratingTopUpLink() {
        response = useCase.execute(topUpRequest());
    }

    private void whenGeneratingTopUpLinkFails() {
        thrownException = assertThrows(RuntimeException.class, () ->
                useCase.execute(topUpRequest()));
    }

    private void thenPaymentUrlIsReturned() {
        assertEquals(paymentUrl, response.paymentUrl());
    }

    private void thenPaymentProviderWasCalled() {
        verify(externalPaymentService).createTopUpPreference(studentId, amount);
    }

    private void thenGenerationFailsWith(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private void thenPaymentProviderWasNotCalled() {
        verify(externalPaymentService, never()).createTopUpPreference(studentId, amount);
    }

    private GenerateTopUpLinkRequest topUpRequest() {
        return new GenerateTopUpLinkRequest(studentId, amount);
    }
}
