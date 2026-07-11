package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GetHistoryByStudent;
import com.recred.backend.application.GetHistoryByStudentRequest;
import com.recred.backend.application.GetHistoryByStudentResponse;
import com.recred.backend.application.GetPendingHistoryByStudent;
import com.recred.backend.application.GetPendingHistoryByStudentRequest;
import com.recred.backend.application.GetPendingHistoryByStudentResponse;
import com.recred.backend.application.GetTutorHistory;
import com.recred.backend.application.GetTutorHistoryRequest;
import com.recred.backend.application.GetTutorHistoryResponse;
import com.recred.backend.infrastructure.web.PurchaseHistoryController;
import com.recred.backend.infrastructure.web.PurchaseResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseHistoryControllerTest {

    private static final String COGNITO_SUBJECT = "cognito-sub";

    @Mock
    private GetHistoryByStudent getHistoryByStudent;
    @Mock
    private GetTutorHistory getTutorHistory;
    @Mock
    private GetPendingHistoryByStudent getPendingHistoryByStudent;

    private PurchaseHistoryController controller;

    @BeforeEach
    void setUp() {
        controller = new PurchaseHistoryController(
                getHistoryByStudent,
                getTutorHistory,
                getPendingHistoryByStudent
        );
    }

    @Test
    void getHistorialAlumnoShouldReturnOkAndMappedPurchases() {
        UUID studentId = UUID.randomUUID();
        UUID purchaseId = UUID.randomUUID();
        givenStudentHistory(studentId, purchaseId);

        ResponseEntity<List<PurchaseResponseDTO>> response = controller.getHistorialAlumno(studentId);

        thenResponseContainsPurchaseAndStatusIsOk(response, purchaseId, "ENTREGADO", new BigDecimal("1500.50"));
        thenDelegatesToGetHistoryByStudent(studentId);
    }

    @Test
    void getHistorialTutorShouldReturnOkAndMappedPurchases() {
        UUID tutorId = UUID.randomUUID();
        UUID purchaseId = UUID.randomUUID();
        givenTutorHistory(tutorId, purchaseId);

        ResponseEntity<List<PurchaseResponseDTO>> response = controller.getHistorialTutor(tutorId);

        thenResponseContainsPurchaseAndStatusIsOk(response, purchaseId, "EN_PREPARACION", new BigDecimal("3000.00"));
        thenDelegatesToGetTutorHistory(tutorId);
    }

    @Test
    void getPendientesAlumnoShouldReturnOkAndMappedPurchases() {
        UUID studentId = UUID.randomUUID();
        UUID purchaseId = UUID.randomUUID();
        givenPendingStudentHistory(studentId, purchaseId);

        ResponseEntity<List<PurchaseResponseDTO>> response = controller.getPendientesAlumno(studentId);

        thenResponseContainsPurchaseAndStatusIsOk(response, purchaseId, "PENDIENTE", new BigDecimal("1200.00"));
        thenDelegatesToGetPendingHistoryByStudent(studentId);
    }

    @Test
    void getMyHistorialShouldUseAuthenticatedSubjectAndReturnOk() {
        UUID purchaseId = UUID.randomUUID();
        givenAuthenticatedTutorHistory(purchaseId);

        ResponseEntity<List<PurchaseResponseDTO>> response = controller.getMyHistorial(
                new TestingAuthenticationToken(COGNITO_SUBJECT, null)
        );

        thenResponseContainsPurchaseAndStatusIsOk(response, purchaseId, "ENTREGADO", new BigDecimal("2200.00"));
        thenDelegatesToGetMyHistory();
    }

    @Test
    void getMyHistorialWithoutAuthenticationShouldFail() {
        Executable execution = () -> controller.getMyHistorial(null);

        thenThrowsAuthenticationCredentialsNotFoundException(execution);
    }

    private void givenStudentHistory(UUID studentId, UUID purchaseId) {
        GetHistoryByStudentResponse response = new GetHistoryByStudentResponse(
                purchaseId,
                studentId,
                new BigDecimal("1500.50"),
                "ENTREGADO",
                "Entregado",
                "EFECTIVO",
                LocalDateTime.now(),
                List.of(),
                "PRESENCIAL",
                null,
                null,
                null,
                null,
                null
        );
        when(getHistoryByStudent.execute(new GetHistoryByStudentRequest(studentId)))
                .thenReturn(List.of(response));
    }

    private void givenTutorHistory(UUID tutorId, UUID purchaseId) {
        GetHistoryByStudentResponse response = new GetHistoryByStudentResponse(
                purchaseId,
                null,
                new BigDecimal("3000.00"),
                "EN_PREPARACION",
                "En preparacion",
                null,
                LocalDateTime.now(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null
        );
        when(getTutorHistory.execute(GetTutorHistoryRequest.byTutorId(tutorId)))
                .thenReturn(new GetTutorHistoryResponse(List.of(response)));
    }

    private void givenPendingStudentHistory(UUID studentId, UUID purchaseId) {
        GetPendingHistoryByStudentResponse response = new GetPendingHistoryByStudentResponse(
                purchaseId,
                studentId,
                new BigDecimal("1200.00"),
                "PENDIENTE",
                "Pendiente",
                "EFECTIVO",
                LocalDateTime.now(),
                List.of(),
                "PRESENCIAL",
                null,
                null,
                null,
                null,
                null
        );
        when(getPendingHistoryByStudent.execute(new GetPendingHistoryByStudentRequest(studentId)))
                .thenReturn(List.of(response));
    }

    private void givenAuthenticatedTutorHistory(UUID purchaseId) {
        GetHistoryByStudentResponse response = new GetHistoryByStudentResponse(
                purchaseId,
                null,
                new BigDecimal("2200.00"),
                "ENTREGADO",
                "Entregado",
                null,
                LocalDateTime.now(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null
        );
        when(getTutorHistory.execute(GetTutorHistoryRequest.byCognitoSubject(COGNITO_SUBJECT)))
                .thenReturn(new GetTutorHistoryResponse(List.of(response)));
    }

    private void thenResponseContainsPurchaseAndStatusIsOk(
            ResponseEntity<List<PurchaseResponseDTO>> response,
            UUID expectedPurchaseId,
            String expectedStatus,
            BigDecimal expectedAmount
    ) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(expectedPurchaseId, response.getBody().get(0).getId());
        assertEquals(expectedAmount, response.getBody().get(0).getTotalAmount());
        assertEquals(expectedStatus, response.getBody().get(0).getStatus());
    }

    private void thenDelegatesToGetHistoryByStudent(UUID studentId) {
        verify(getHistoryByStudent).execute(new GetHistoryByStudentRequest(studentId));
    }

    private void thenDelegatesToGetTutorHistory(UUID tutorId) {
        verify(getTutorHistory).execute(GetTutorHistoryRequest.byTutorId(tutorId));
    }

    private void thenDelegatesToGetPendingHistoryByStudent(UUID studentId) {
        verify(getPendingHistoryByStudent).execute(new GetPendingHistoryByStudentRequest(studentId));
    }

    private void thenDelegatesToGetMyHistory() {
        verify(getTutorHistory).execute(GetTutorHistoryRequest.byCognitoSubject(COGNITO_SUBJECT));
    }

    private void thenThrowsAuthenticationCredentialsNotFoundException(Executable execution) {
        assertThrows(AuthenticationCredentialsNotFoundException.class, execution);
    }
}
