package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.ActivateUserSubscription;
import com.recred.backend.application.CreateUserSubscriptionPayment;
import com.recred.backend.application.CreateUserSubscriptionPaymentRequest;
import com.recred.backend.application.CreateUserSubscriptionPaymentResponse;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.infrastructure.web.UserSubscriptionPaymentController;
import com.recred.backend.infrastructure.web.UserSubscriptionPaymentRequest;
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

    @Test
    void createPayment_ShouldDelegateAndReturnPaymentData() {
        CreateUserSubscriptionPayment useCase = mock(CreateUserSubscriptionPayment.class);
        ActivateUserSubscription activateUserSubscription = mock(ActivateUserSubscription.class);
        UserSubscriptionPaymentController controller =
                new UserSubscriptionPaymentController(useCase, activateUserSubscription);
        UUID userId = UUID.randomUUID();
        CreateUserSubscriptionPaymentResponse useCaseResponse = response();
        givenUseCaseResponse(useCase, useCaseResponse);

        ResponseEntity<CreateUserSubscriptionPaymentResponse> response =
                whenCreatingPayment(controller, userId);

        CreateUserSubscriptionPaymentRequest request = capturedRequest(useCase);
        assertEquals(userId, request.usuarioId());
        assertEquals(TipoPlan.AVANZADO, request.plan());
        assertEquals(PeriodoSuscripcion.ANUAL, request.periodo());
        assertEquals(useCaseResponse, response.getBody());
    }

    private void givenUseCaseResponse(
            CreateUserSubscriptionPayment useCase,
            CreateUserSubscriptionPaymentResponse response
    ) {
        when(useCase.execute(org.mockito.ArgumentMatchers.any(
                CreateUserSubscriptionPaymentRequest.class))).thenReturn(response);
    }

    private ResponseEntity<CreateUserSubscriptionPaymentResponse> whenCreatingPayment(
            UserSubscriptionPaymentController controller,
            UUID userId
    ) {
        return controller.createPayment(new UserSubscriptionPaymentRequest(
                userId,
                TipoPlan.AVANZADO,
                PeriodoSuscripcion.ANUAL
        ));
    }

    private CreateUserSubscriptionPaymentRequest capturedRequest(
            CreateUserSubscriptionPayment useCase
    ) {
        ArgumentCaptor<CreateUserSubscriptionPaymentRequest> captor =
                ArgumentCaptor.forClass(CreateUserSubscriptionPaymentRequest.class);
        verify(useCase).execute(captor.capture());
        return captor.getValue();
    }

    private CreateUserSubscriptionPaymentResponse response() {
        return new CreateUserSubscriptionPaymentResponse(
                "https://pay.test/subscription",
                TipoPlan.AVANZADO,
                PeriodoSuscripcion.ANUAL,
                new BigDecimal("97920.00"),
                "ARS"
        );
    }
}
