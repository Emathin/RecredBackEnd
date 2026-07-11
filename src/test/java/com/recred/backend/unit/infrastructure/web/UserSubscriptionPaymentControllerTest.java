package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.ActivateUserSubscription;
import com.recred.backend.application.CreateUserSubscriptionPayment;
import com.recred.backend.application.CreateUserSubscriptionPaymentRequest;
import com.recred.backend.application.CreateUserSubscriptionPaymentResponse;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.infrastructure.web.UserSubscriptionPaymentController;
import com.recred.backend.infrastructure.web.UserSubscriptionPaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserSubscriptionPaymentControllerTest {

    private final UUID userId = UUID.randomUUID();

    private CreateUserSubscriptionPayment useCase;
    private ActivateUserSubscription activateUserSubscription;
    private UserSubscriptionPaymentController controller;
    private ResponseEntity<CreateUserSubscriptionPaymentResponse> response;

    @BeforeEach
    void setUp() {
        useCase = mock(CreateUserSubscriptionPayment.class);
        activateUserSubscription = mock(ActivateUserSubscription.class);
        controller = new UserSubscriptionPaymentController(
                useCase, activateUserSubscription);
    }

    @Test
    void givenValidRequestWhenCreatingPaymentThenDelegatesAndReturnsPaymentData() {
        givenUseCaseReturnsResponse();

        whenCreatingPayment();

        thenRequestContainsUserId();
        thenRequestContainsPlan(TipoPlan.AVANZADO);
        thenRequestContainsPeriod(PeriodoSuscripcion.ANUAL);
        thenResponseBodyMatchesUseCaseResponse();
    }

    private void givenUseCaseReturnsResponse() {
        when(useCase.execute(org.mockito.ArgumentMatchers.any(
                CreateUserSubscriptionPaymentRequest.class)))
                .thenReturn(expectedResponse());
    }

    private void whenCreatingPayment() {
        response = controller.createPayment(
                new UserSubscriptionPaymentRequest(
                        userId, TipoPlan.AVANZADO, PeriodoSuscripcion.ANUAL));
    }

    private void thenRequestContainsUserId() {
        assertEquals(userId, capturedRequest().usuarioId());
    }

    private void thenRequestContainsPlan(TipoPlan expected) {
        assertEquals(expected, capturedRequest().plan());
    }

    private void thenRequestContainsPeriod(PeriodoSuscripcion expected) {
        assertEquals(expected, capturedRequest().periodo());
    }

    private void thenResponseBodyMatchesUseCaseResponse() {
        assertEquals(expectedResponse(), response.getBody());
    }

    private CreateUserSubscriptionPaymentRequest capturedRequest() {
        ArgumentCaptor<CreateUserSubscriptionPaymentRequest> captor =
                ArgumentCaptor.forClass(
                        CreateUserSubscriptionPaymentRequest.class);
        verify(useCase).execute(captor.capture());
        return captor.getValue();
    }

    private CreateUserSubscriptionPaymentResponse expectedResponse() {
        return new CreateUserSubscriptionPaymentResponse(
                "https://pay.test/subscription",
                TipoPlan.AVANZADO,
                PeriodoSuscripcion.ANUAL,
                new BigDecimal("97920.00"),
                "ARS"
        );
    }
}
