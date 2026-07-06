package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.CancelPresentialPurchase;
import com.recred.backend.application.CreateAdvancePurchase;
import com.recred.backend.application.CreateAdvancePurchaseRequest;
import com.recred.backend.application.CreateAdvancePurchaseResponse;
import com.recred.backend.application.GetPresentialPurchaseRequest;
import com.recred.backend.application.GetPresentialPurchaseResponse;
import com.recred.backend.application.GetPresentialPurchase;
import com.recred.backend.application.GetPurchaseStatus;
import com.recred.backend.application.GetPurchaseStatusRequest;
import com.recred.backend.application.GetPurchaseStatusResponse;
import com.recred.backend.application.ProcessPresentialPurchase;
import com.recred.backend.application.ProcessPresentialPurchaseRequest;
import com.recred.backend.application.ProcessPresentialPurchaseResponse;
import com.recred.backend.application.UpdatePurchaseStatus;
import com.recred.backend.application.UpdatePurchaseStatusRequest;
import com.recred.backend.application.UpdatePurchaseStatusResponse;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseStatus;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.infrastructure.web.AdvancePurchaseRequest;
import com.recred.backend.infrastructure.web.AdvancePurchaseResponse;
import com.recred.backend.infrastructure.web.KiosqueroAccessGuard;
import com.recred.backend.infrastructure.web.ProcessPurchaseRequestDTO;
import com.recred.backend.infrastructure.web.PurchaseAccessGuard;
import com.recred.backend.infrastructure.web.PurchaseController;
import com.recred.backend.infrastructure.web.PurchaseItemDTO;
import com.recred.backend.infrastructure.web.PurchaseResponseDTO;
import com.recred.backend.infrastructure.web.PurchaseStatusResponseDTO;
import com.recred.backend.infrastructure.web.UpdatePurchaseStatusRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private com.recred.backend.application.RevertPurchaseCancellation revertPurchaseCancellation;
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
                revertPurchaseCancellation,
                createAdvancePurchase,
                kiosqueroAccessGuard,
                purchaseAccessGuard
        );
    }

    @Test
    void processPurchase_ShouldRequireBuffetAccessAndDelegateWithItems() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID purchaseId = UUID.randomUUID();
        Authentication authentication = mock(Authentication.class);
        ProcessPurchaseRequestDTO request = new ProcessPurchaseRequestDTO(
                studentId,
                buffetId,
                List.of(new PurchaseItemDTO(productId, "Alfajor", 2, BigDecimal.TEN))
        );
        ProcessPresentialPurchaseRequest useCaseRequest =
                new ProcessPresentialPurchaseRequest(
                        studentId,
                        List.of(new ProcessPresentialPurchaseRequest.ItemRequest(productId, 2)),
                        buffetId
                );
        when(processPresentialPurchase.execute(useCaseRequest))
                .thenReturn(new ProcessPresentialPurchaseResponse(
                        purchaseId,
                        new BigDecimal("20.00"),
                        "ENTREGADO"
                ));

        ResponseEntity<ProcessPresentialPurchaseResponse> response =
                controller.processPurchase(request, authentication);

        assertEquals(purchaseId, response.getBody().purchaseId());
        assertEquals(new BigDecimal("20.00"), response.getBody().totalAmount());
        verify(kiosqueroAccessGuard).requireBuffetAccess(authentication, buffetId);
        verify(processPresentialPurchase).execute(useCaseRequest);
    }

    @Test
    void createAdvancePurchase_ShouldAttachCognitoSubjectAndMapResponse() {
        UUID studentId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        UUID purchaseId = UUID.randomUUID();
        Authentication authentication = authentication();
        AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of(
                new AdvancePurchaseRequest.OrderRequest(
                        studentId,
                        buyerId,
                        BuyerType.TUTOR,
                        buffetId,
                        "2026-07-03",
                        RecessTime.FIRST_RECESS,
                        List.of(new AdvancePurchaseRequest.ItemRequest(productId, 3)),
                        suggestionId,
                        tutorId
                )
        ));
        CreateAdvancePurchaseRequest useCaseRequest = new CreateAdvancePurchaseRequest(
                COGNITO_SUBJECT,
                List.of(new CreateAdvancePurchaseRequest.OrderRequest(
                        studentId,
                        buyerId,
                        BuyerType.TUTOR,
                        buffetId,
                        "2026-07-03",
                        RecessTime.FIRST_RECESS,
                        List.of(new CreateAdvancePurchaseRequest.ItemRequest(productId, 3)),
                        suggestionId,
                        tutorId
                ))
        );
        PurchaseStatus status = new PurchaseStatus(
                purchaseId,
                studentId,
                EstadoCompra.PENDIENTE,
                "Pendiente",
                LocalDateTime.of(2026, 7, 3, 9, 0)
        );
        when(createAdvancePurchase.execute(useCaseRequest))
                .thenReturn(new CreateAdvancePurchaseResponse(
                        "ORD-1",
                        status,
                        1500.0,
                        Map.of(studentId, "ABC123")
                ));

        ResponseEntity<AdvancePurchaseResponse> response =
                controller.createAdvancePurchase(request, authentication);

        assertEquals("ORD-1", response.getBody().orderId());
        assertEquals(status, response.getBody().status());
        assertEquals(1500.0, response.getBody().total());
        assertEquals("ABC123", response.getBody().codes().get(studentId));
        verify(createAdvancePurchase).execute(useCaseRequest);
    }

    @Test
    void createAdvancePurchaseWithoutAuthentication_ShouldThrowException() {
        AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of());

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.createAdvancePurchase(request, null));
    }

    @Test
    void getPurchaseById_ShouldRequireAccessAndMapResponse() {
        UUID purchaseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LocalDateTime date = LocalDateTime.of(2026, 7, 3, 10, 0);
        Authentication authentication = mock(Authentication.class);
        Purchase purchase = new Purchase();
        purchase.setId(purchaseId);
        givenPurchaseCanBeAccessed(authentication, purchaseId, purchase);
        when(getPresentialPurchase.execute(new GetPresentialPurchaseRequest(purchaseId)))
                .thenReturn(new GetPresentialPurchaseResponse(
                        purchaseId,
                        studentId,
                        new BigDecimal("250.00"),
                        "ENTREGADO",
                        "Entregado",
                        "CREDITOS",
                        date,
                        "PRESENCIAL",
                        "CODE-1",
                        "Primer recreo",
                        LocalDate.of(2026, 7, 3),
                        LocalTime.of(9, 30),
                        List.of(new GetPresentialPurchaseResponse.ItemResponse(
                                productId,
                                "Alfajor",
                                2,
                                new BigDecimal("125.00"),
                                false,
                                20,
                                null,
                                null
                        ))
                ));

        ResponseEntity<PurchaseResponseDTO> response =
                controller.getPurchaseById(purchaseId, authentication);

        assertEquals(purchaseId, response.getBody().getId());
        assertEquals(studentId, response.getBody().getStudentId());
        assertEquals("ENTREGADO", response.getBody().getStatus());
        assertEquals("09:30", response.getBody().getPickupSlotStartTime());
        assertEquals(1, response.getBody().getItems().size());
        assertEquals(productId, response.getBody().getItems().get(0).getProductId());
        verify(purchaseAccessGuard).requirePurchaseAccess(authentication, purchaseId);
        verify(getPresentialPurchase).execute(new GetPresentialPurchaseRequest(purchaseId));
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

    @Test
    void cancelPurchase_ShouldRequireAccessAndDelegateWithRequest() {
        UUID purchaseId = UUID.randomUUID();
        Authentication authentication = mock(Authentication.class);

        ResponseEntity<Void> response = controller.cancelPurchase(purchaseId, authentication);

        assertEquals(200, response.getStatusCode().value());
        verify(purchaseAccessGuard).requirePurchaseAccess(authentication, purchaseId);
        verify(cancelPresentialPurchase).execute(
                new com.recred.backend.application.CancelPresentialPurchaseRequest(purchaseId));
    }

    @Test
    void revertPurchase_ShouldRequireAccessAndDelegateWithRequest() {
        UUID purchaseId = UUID.randomUUID();
        Authentication authentication = mock(Authentication.class);

        ResponseEntity<Void> response = controller.revertPurchase(purchaseId, authentication);

        assertEquals(200, response.getStatusCode().value());
        verify(purchaseAccessGuard).requirePurchaseAccess(authentication, purchaseId);
        verify(revertPurchaseCancellation).execute(
                new com.recred.backend.application.RevertPurchaseCancellationRequest(purchaseId));
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
