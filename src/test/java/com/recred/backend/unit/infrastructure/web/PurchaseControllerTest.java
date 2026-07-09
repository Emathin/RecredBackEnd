package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.CancelPresentialPurchase;
import com.recred.backend.application.CreateAdvancePurchase;
import com.recred.backend.application.CreateAdvancePurchaseRequest;
import com.recred.backend.application.CreateAdvancePurchaseResponse;
import com.recred.backend.application.GetPresentialPurchase;
import com.recred.backend.application.GetPresentialPurchaseRequest;
import com.recred.backend.application.GetPresentialPurchaseResponse;
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
import org.junit.jupiter.api.function.Executable;
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
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID BUFFET_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID PURCHASE_ID = UUID.randomUUID();
    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID SUGGESTION_ID = UUID.randomUUID();
    private static final UUID TUTOR_ID = UUID.randomUUID();

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
    private Authentication authentication;

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
        authentication = mock(Authentication.class);
    }

    @Test
    void processPurchase_ShouldRequireBuffetAccessAndDelegateWithItems() {
        ProcessPurchaseRequestDTO request = buildProcessPurchaseRequest();
        ProcessPresentialPurchaseRequest useCaseRequest = buildProcessPurchaseUseCaseRequest();
        givenProcessPresentialPurchaseReturns(useCaseRequest);

        ResponseEntity<ProcessPresentialPurchaseResponse> response =
                controller.processPurchase(request, authentication);

        thenResponseContainsProcessPurchaseData(response);
        thenBuffetAccessIsRequired();
        thenDelegatesToProcessPresentialPurchase(useCaseRequest);
    }

    @Test
    void createAdvancePurchase_ShouldAttachCognitoSubjectAndMapResponse() {
        givenAuthenticationSubject();
        AdvancePurchaseRequest request = buildAdvancePurchaseRequest();
        CreateAdvancePurchaseRequest useCaseRequest = buildCreateAdvancePurchaseUseCaseRequest();
        PurchaseStatus status = buildPurchaseStatus();
        givenCreateAdvancePurchaseReturns(useCaseRequest, status);

        ResponseEntity<AdvancePurchaseResponse> response =
                controller.createAdvancePurchase(request, authentication);

        thenResponseContainsAdvancePurchaseData(response, status);
        thenDelegatesToCreateAdvancePurchase(useCaseRequest);
    }

    @Test
    void createAdvancePurchaseWithoutAuthentication_ShouldThrowException() {
        AdvancePurchaseRequest request = new AdvancePurchaseRequest(List.of());

        Executable execution = () -> controller.createAdvancePurchase(request, null);

        thenThrowsAuthenticationCredentialsNotFoundException(execution);
    }

    @Test
    void getPurchaseById_ShouldRequireAccessAndMapResponse() {
        givenPurchaseCanBeAccessed();
        givenGetPresentialPurchaseReturns();

        ResponseEntity<PurchaseResponseDTO> response =
                controller.getPurchaseById(PURCHASE_ID, authentication);

        thenResponseContainsGetPurchaseData(response);
        thenPurchaseAccessIsRequired();
        thenDelegatesToGetPresentialPurchase();
    }

    @Test
    void getPurchaseStatusById_ShouldDelegateWithRequestAndMapResponse() {
        givenAuthenticationSubject();
        givenPurchaseCanBeAccessed();
        GetPurchaseStatusRequest request = new GetPurchaseStatusRequest(COGNITO_SUBJECT, PURCHASE_ID);
        givenPurchaseStatusFound(request);

        ResponseEntity<PurchaseStatusResponseDTO> response =
                controller.getPurchaseStatusById(PURCHASE_ID, authentication);

        thenResponseContainsPurchaseStatusData(response);
        thenPurchaseAccessIsRequired();
        thenDelegatesToGetPurchaseStatus(request);
    }

    @Test
    void updatePurchaseStatus_ShouldRequireAccessAndDelegateWithRequest() {
        givenPurchaseCanBeAccessed();
        UpdatePurchaseStatusRequest request =
                new UpdatePurchaseStatusRequest(PURCHASE_ID, EstadoCompra.LISTO);
        givenPurchaseStatusUpdated(request);

        ResponseEntity<PurchaseStatusResponseDTO> response = controller.updatePurchaseStatus(
                PURCHASE_ID,
                new UpdatePurchaseStatusRequestDTO(EstadoCompra.LISTO),
                authentication
        );

        thenResponseContainsUpdatedPurchaseStatusData(response);
        thenPurchaseAccessIsRequired();
        thenBuffetAccessIsRequired();
        thenDelegatesToUpdatePurchaseStatus(request);
    }

    @Test
    void cancelPurchase_ShouldRequireAccessAndDelegateWithRequest() {
        ResponseEntity<Void> response = controller.cancelPurchase(PURCHASE_ID, authentication);

        thenResponseIsOk(response);
        thenPurchaseAccessIsRequired();
        thenDelegatesToCancelPurchase();
    }

    @Test
    void revertPurchase_ShouldRequireAccessAndDelegateWithRequest() {
        ResponseEntity<Void> response = controller.revertPurchase(PURCHASE_ID, authentication);

        thenResponseIsOk(response);
        thenPurchaseAccessIsRequired();
        thenDelegatesToRevertPurchaseCancellation();
    }

    private void givenAuthenticationSubject() {
        when(authentication.getName()).thenReturn(COGNITO_SUBJECT);
    }

    private void givenProcessPresentialPurchaseReturns(ProcessPresentialPurchaseRequest request) {
        when(processPresentialPurchase.execute(request))
                .thenReturn(new ProcessPresentialPurchaseResponse(
                        PURCHASE_ID,
                        new BigDecimal("20.00"),
                        "ENTREGADO"
                ));
    }

    private void givenCreateAdvancePurchaseReturns(
            CreateAdvancePurchaseRequest request,
            PurchaseStatus status
    ) {
        when(createAdvancePurchase.execute(request))
                .thenReturn(new CreateAdvancePurchaseResponse(
                        "ORD-1",
                        status,
                        1500.0,
                        Map.of(STUDENT_ID, "ABC123")
                ));
    }

    private void givenPurchaseCanBeAccessed() {
        Purchase purchase = new Purchase();
        purchase.setId(PURCHASE_ID);
        purchase.setBuffetId(BUFFET_ID);
        when(purchaseAccessGuard.requirePurchaseAccess(authentication, PURCHASE_ID))
                .thenReturn(purchase);
    }

    private void givenGetPresentialPurchaseReturns() {
        LocalDateTime date = LocalDateTime.of(2026, 7, 3, 10, 0);
        when(getPresentialPurchase.execute(new GetPresentialPurchaseRequest(PURCHASE_ID)))
                .thenReturn(new GetPresentialPurchaseResponse(
                        PURCHASE_ID,
                        STUDENT_ID,
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
                                PRODUCT_ID,
                                "Alfajor",
                                2,
                                new BigDecimal("125.00"),
                                false,
                                20,
                                null,
                                null
                        ))
                ));
    }

    private void givenPurchaseStatusFound(GetPurchaseStatusRequest request) {
        LocalDateTime date = LocalDateTime.of(2026, 6, 29, 10, 0);
        when(getPurchaseStatus.execute(request)).thenReturn(new GetPurchaseStatusResponse(
                PURCHASE_ID,
                STUDENT_ID,
                "LISTO",
                "Listo para retirar",
                date
        ));
    }

    private void givenPurchaseStatusUpdated(UpdatePurchaseStatusRequest request) {
        LocalDateTime date = LocalDateTime.of(2026, 6, 29, 10, 0);
        when(updatePurchaseStatus.execute(request)).thenReturn(new UpdatePurchaseStatusResponse(
                PURCHASE_ID,
                STUDENT_ID,
                "LISTO",
                "Listo para retirar",
                date
        ));
    }

    private ProcessPurchaseRequestDTO buildProcessPurchaseRequest() {
        return new ProcessPurchaseRequestDTO(
                STUDENT_ID,
                BUFFET_ID,
                List.of(new PurchaseItemDTO(PRODUCT_ID, "Alfajor", 2, BigDecimal.TEN))
        );
    }

    private ProcessPresentialPurchaseRequest buildProcessPurchaseUseCaseRequest() {
        return new ProcessPresentialPurchaseRequest(
                STUDENT_ID,
                List.of(new ProcessPresentialPurchaseRequest.ItemRequest(PRODUCT_ID, 2)),
                BUFFET_ID
        );
    }

    private AdvancePurchaseRequest buildAdvancePurchaseRequest() {
        return new AdvancePurchaseRequest(List.of(
                new AdvancePurchaseRequest.OrderRequest(
                        STUDENT_ID,
                        BUYER_ID,
                        BuyerType.TUTOR,
                        BUFFET_ID,
                        "2026-07-03",
                        RecessTime.FIRST_RECESS,
                        List.of(new AdvancePurchaseRequest.ItemRequest(PRODUCT_ID, 3)),
                        SUGGESTION_ID,
                        TUTOR_ID
                )
        ));
    }

    private CreateAdvancePurchaseRequest buildCreateAdvancePurchaseUseCaseRequest() {
        return new CreateAdvancePurchaseRequest(
                COGNITO_SUBJECT,
                List.of(new CreateAdvancePurchaseRequest.OrderRequest(
                        STUDENT_ID,
                        BUYER_ID,
                        BuyerType.TUTOR,
                        BUFFET_ID,
                        "2026-07-03",
                        RecessTime.FIRST_RECESS,
                        List.of(new CreateAdvancePurchaseRequest.ItemRequest(PRODUCT_ID, 3)),
                        SUGGESTION_ID,
                        TUTOR_ID
                ))
        );
    }

    private PurchaseStatus buildPurchaseStatus() {
        return new PurchaseStatus(
                PURCHASE_ID,
                STUDENT_ID,
                EstadoCompra.PENDIENTE,
                "Pendiente",
                LocalDateTime.of(2026, 7, 3, 9, 0)
        );
    }

    private void thenResponseContainsProcessPurchaseData(
            ResponseEntity<ProcessPresentialPurchaseResponse> response
    ) {
        assertEquals(PURCHASE_ID, response.getBody().purchaseId());
        assertEquals(new BigDecimal("20.00"), response.getBody().totalAmount());
    }

    private void thenResponseContainsAdvancePurchaseData(
            ResponseEntity<AdvancePurchaseResponse> response,
            PurchaseStatus status
    ) {
        assertEquals("ORD-1", response.getBody().orderId());
        assertEquals(status, response.getBody().status());
        assertEquals(1500.0, response.getBody().total());
        assertEquals("ABC123", response.getBody().codes().get(STUDENT_ID));
    }

    private void thenResponseContainsGetPurchaseData(ResponseEntity<PurchaseResponseDTO> response) {
        assertEquals(PURCHASE_ID, response.getBody().getId());
        assertEquals(STUDENT_ID, response.getBody().getStudentId());
        assertEquals("ENTREGADO", response.getBody().getStatus());
        assertEquals("09:30", response.getBody().getPickupSlotStartTime());
        assertEquals(1, response.getBody().getItems().size());
        assertEquals(PRODUCT_ID, response.getBody().getItems().get(0).getProductId());
    }

    private void thenResponseContainsPurchaseStatusData(
            ResponseEntity<PurchaseStatusResponseDTO> response
    ) {
        LocalDateTime expectedDate = LocalDateTime.of(2026, 6, 29, 10, 0);
        assertEquals(PURCHASE_ID, response.getBody().purchaseId());
        assertEquals(STUDENT_ID, response.getBody().studentId());
        assertEquals("LISTO", response.getBody().status());
        assertEquals("Listo para retirar", response.getBody().statusLabel());
        assertEquals(expectedDate, response.getBody().date());
    }

    private void thenResponseContainsUpdatedPurchaseStatusData(
            ResponseEntity<PurchaseStatusResponseDTO> response
    ) {
        assertEquals("LISTO", response.getBody().status());
        assertEquals("Listo para retirar", response.getBody().statusLabel());
    }

    private void thenResponseIsOk(ResponseEntity<Void> response) {
        assertEquals(200, response.getStatusCode().value());
    }

    private void thenThrowsAuthenticationCredentialsNotFoundException(Executable execution) {
        assertThrows(AuthenticationCredentialsNotFoundException.class, execution);
    }

    private void thenBuffetAccessIsRequired() {
        verify(kiosqueroAccessGuard).requireBuffetAccess(authentication, BUFFET_ID);
    }

    private void thenPurchaseAccessIsRequired() {
        verify(purchaseAccessGuard).requirePurchaseAccess(authentication, PURCHASE_ID);
    }

    private void thenDelegatesToProcessPresentialPurchase(ProcessPresentialPurchaseRequest request) {
        verify(processPresentialPurchase).execute(request);
    }

    private void thenDelegatesToCreateAdvancePurchase(CreateAdvancePurchaseRequest request) {
        verify(createAdvancePurchase).execute(request);
    }

    private void thenDelegatesToGetPresentialPurchase() {
        verify(getPresentialPurchase).execute(new GetPresentialPurchaseRequest(PURCHASE_ID));
    }

    private void thenDelegatesToGetPurchaseStatus(GetPurchaseStatusRequest request) {
        verify(getPurchaseStatus).execute(request);
    }

    private void thenDelegatesToUpdatePurchaseStatus(UpdatePurchaseStatusRequest request) {
        verify(updatePurchaseStatus).execute(request);
    }

    private void thenDelegatesToCancelPurchase() {
        verify(cancelPresentialPurchase).execute(
                new com.recred.backend.application.CancelPresentialPurchaseRequest(PURCHASE_ID));
    }

    private void thenDelegatesToRevertPurchaseCancellation() {
        verify(revertPurchaseCancellation).execute(
                new com.recred.backend.application.RevertPurchaseCancellationRequest(PURCHASE_ID));
    }
}
