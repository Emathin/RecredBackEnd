package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.CreateSchoolSubscriptionPayment;
import com.recred.backend.application.CreateSchoolSubscriptionPaymentRequest;
import com.recred.backend.application.CreateSchoolSubscriptionPaymentResponse;
import com.recred.backend.infrastructure.web.SchoolSubscriptionPaymentController;
import com.recred.backend.infrastructure.web.SchoolSubscriptionPaymentRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchoolSubscriptionPaymentControllerTest {

    @Test
    void createPayment_ShouldDelegateAndReturnPaymentData() {
        CreateSchoolSubscriptionPayment useCase = mock(CreateSchoolSubscriptionPayment.class);
        SchoolSubscriptionPaymentController controller =
                new SchoolSubscriptionPaymentController(useCase);
        UUID schoolId = UUID.randomUUID();
        CreateSchoolSubscriptionPaymentResponse useCaseResponse = response();
        givenUseCaseResponse(useCase, useCaseResponse);

        ResponseEntity<CreateSchoolSubscriptionPaymentResponse> response =
                whenCreatingPayment(controller, schoolId);

        CreateSchoolSubscriptionPaymentRequest request = capturedRequest(useCase);
        assertEquals(schoolId, request.colegioId());
        assertEquals(useCaseResponse, response.getBody());
    }

    private void givenUseCaseResponse(
            CreateSchoolSubscriptionPayment useCase,
            CreateSchoolSubscriptionPaymentResponse response
    ) {
        when(useCase.execute(org.mockito.ArgumentMatchers.any(
                CreateSchoolSubscriptionPaymentRequest.class))).thenReturn(response);
    }

    private ResponseEntity<CreateSchoolSubscriptionPaymentResponse> whenCreatingPayment(
            SchoolSubscriptionPaymentController controller,
            UUID schoolId
    ) {
        return controller.createPayment(new SchoolSubscriptionPaymentRequest(schoolId));
    }

    private CreateSchoolSubscriptionPaymentRequest capturedRequest(
            CreateSchoolSubscriptionPayment useCase
    ) {
        ArgumentCaptor<CreateSchoolSubscriptionPaymentRequest> captor =
                ArgumentCaptor.forClass(CreateSchoolSubscriptionPaymentRequest.class);
        verify(useCase).execute(captor.capture());
        return captor.getValue();
    }

    private CreateSchoolSubscriptionPaymentResponse response() {
        return new CreateSchoolSubscriptionPaymentResponse(
                "https://pay.test/school",
                new BigDecimal("20.00"),
                "USD"
        );
    }
}
