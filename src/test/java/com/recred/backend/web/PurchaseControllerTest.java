package com.recred.backend.web;

import com.recred.backend.application.CancelPresentialPurchase;
import com.recred.backend.application.CreateAdvancePurchase;
import com.recred.backend.application.GetPresentialPurchase;
import com.recred.backend.application.GetPurchaseStatus;
import com.recred.backend.application.GetPurchaseStatusRequest;
import com.recred.backend.application.GetPurchaseStatusResponse;
import com.recred.backend.application.ProcessPresentialPurchase;
import com.recred.backend.application.UpdatePurchaseStatus;
import com.recred.backend.application.UpdatePurchaseStatusRequest;
import com.recred.backend.application.UpdatePurchaseStatusResponse;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.infrastructure.web.KiosqueroAccessGuard;
import com.recred.backend.infrastructure.web.PurchaseAccessGuard;
import com.recred.backend.infrastructure.web.PurchaseController;
import com.recred.backend.infrastructure.web.PurchaseStatusResponseDTO;
import com.recred.backend.infrastructure.web.UpdatePurchaseStatusRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseControllerTest {

    private static final String COGNITO_SUBJECT = "cognito-subject";

    @Mock
    private ProcessPresentialPurchase processPresentialPurchase;
    @Mock
    private GetPresentialPurchase getPresentialPurchase;
    @Mock
    private GetPurchaseStatus getPurchaseStatus;
    @Mock
    private UpdatePurchaseStatus updatePurchaseStatus;
    @Mock
    private CancelPresentialPurchase cancelPresentialPurchase;
    @Mock
    private CreateAdvancePurchase createAdvancePurchase;
    @Mock
    private KiosqueroAccessGuard kiosqueroAccessGuard;
    @Mock
    private PurchaseAccessGuard purchaseAccessGuard;

    private PurchaseController controller;

    @BeforeEach
    void setUp() {
        controller = new PurchaseController(
                processPresentialPurchase,
                getPresentialPurchase,
                getPurchaseStatus,
                updatePurchaseStatus,
                cancelPresentialPurchase,
                createAdvancePurchase,
                kiosqueroAccessGuard,
                purchaseAccessGuard
        );
    }

    @Test
    void getPurchaseStatusById_ShouldDelegateWithRequestAndMapResponse() {
        UUID purchaseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        LocalDateTime date = LocalDateTime.of(2026, 6, 29, 10, 0);
        Authentication authentication = authentication();
        GetPurchaseStatusRequest request =
                new GetPurchaseStatusRequest(COGNITO_SUBJECT, purchaseId);
        GetPurchaseStatusResponse useCaseResponse = new GetPurchaseStatusResponse(
                purchaseId,
                studentId,
                "LISTO",
                "Listo para retirar",
                date
        );
        givenPurchaseStatusFound(request, useCaseResponse);

        ResponseEntity<PurchaseStatusResponseDTO> response =
                controller.getPurchaseStatusById(purchaseId, authentication);

        assertEquals(purchaseId, response.getBody().purchaseId());
        assertEquals(studentId, response.getBody().studentId());
        assertEquals("LISTO", response.getBody().status());
        assertEquals("Listo para retirar", response.getBody().statusLabel());
        assertEquals(date, response.getBody().date());
        verify(getPurchaseStatus).execute(request);
    }

    @Test
    void updatePurchaseStatus_ShouldRequireAccessAndDelegateWithRequest() {
        UUID purchaseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        LocalDateTime date = LocalDateTime.of(2026, 6, 29, 10, 0);
        Authentication authentication = mock(Authentication.class);
        Purchase purchase = purchase(buffetId);
        UpdatePurchaseStatusRequest request =
                new UpdatePurchaseStatusRequest(purchaseId, EstadoCompra.LISTO);
        UpdatePurchaseStatusResponse useCaseResponse = new UpdatePurchaseStatusResponse(
                purchaseId,
                studentId,
                "LISTO",
                "Listo para retirar",
                date
        );
        givenPurchaseCanBeAccessed(authentication, purchaseId, purchase);
        givenPurchaseStatusUpdated(request, useCaseResponse);

        ResponseEntity<PurchaseStatusResponseDTO> response = controller.updatePurchaseStatus(
                purchaseId,
                new UpdatePurchaseStatusRequestDTO(EstadoCompra.LISTO),
                authentication
        );

        assertEquals("LISTO", response.getBody().status());
        assertEquals("Listo para retirar", response.getBody().statusLabel());
        verify(purchaseAccessGuard).requirePurchaseAccess(authentication, purchaseId);
        verify(kiosqueroAccessGuard).requireBuffetAccess(authentication, buffetId);
        verify(updatePurchaseStatus).execute(request);
    }

    private void givenPurchaseStatusFound(
            GetPurchaseStatusRequest request,
            GetPurchaseStatusResponse response
    ) {
        when(getPurchaseStatus.execute(request)).thenReturn(response);
    }

    private void givenPurchaseCanBeAccessed(
            Authentication authentication,
            UUID purchaseId,
            Purchase purchase
    ) {
        when(purchaseAccessGuard.requirePurchaseAccess(authentication, purchaseId))
                .thenReturn(purchase);
    }

    private void givenPurchaseStatusUpdated(
            UpdatePurchaseStatusRequest request,
            UpdatePurchaseStatusResponse response
    ) {
        when(updatePurchaseStatus.execute(request)).thenReturn(response);
    }

    private Authentication authentication() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(COGNITO_SUBJECT);
        return authentication;
    }

    private Purchase purchase(UUID buffetId) {
        Purchase purchase = new Purchase();
        purchase.setBuffetId(buffetId);
        return purchase;
    }
}
