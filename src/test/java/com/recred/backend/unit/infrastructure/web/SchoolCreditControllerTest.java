package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.EnableSchoolCredit;
import com.recred.backend.application.EnableSchoolCreditRequest;
import com.recred.backend.application.EnableSchoolCreditResponse;
import com.recred.backend.application.GetActiveSchoolCredit;
import com.recred.backend.application.GetActiveSchoolCreditRequest;
import com.recred.backend.application.GetActiveSchoolCreditResponse;
import com.recred.backend.application.GetLastRecharge;
import com.recred.backend.application.GetLastRechargeRequest;
import com.recred.backend.application.GetLastRechargeResponse;
import com.recred.backend.application.GetSchoolCreditHistory;
import com.recred.backend.application.PaySchoolCredit;
import com.recred.backend.domain.exceptions.ActiveCreditExistsException;
import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.SchoolCredit;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.services.UsuarioService;
import com.recred.backend.infrastructure.web.EnableSchoolCreditRequestDTO;
import com.recred.backend.infrastructure.web.SchoolCreditController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolCreditControllerTest {

    private static final String AUTHENTICATED_USER = "test-user";

    @Mock private EnableSchoolCredit enableSchoolCredit;
    @Mock private GetLastRecharge getLastRecharge;
    @Mock private GetActiveSchoolCredit getActiveSchoolCredit;
    @Mock private PaySchoolCredit paySchoolCredit;
    @Mock private GetSchoolCreditHistory getSchoolCreditHistory;
    @Mock private UsuarioService usuarioService;

    private SchoolCreditController controller;
    private Authentication authentication;

    private UUID tutorId;
    private UUID studentId;
    private UUID creditId;

    @BeforeEach
    void setUp() {
        controller = new SchoolCreditController(
                enableSchoolCredit,
                paySchoolCredit,
                getSchoolCreditHistory,
                getLastRecharge,
                getActiveSchoolCredit,
                usuarioService
        );
        authentication = new TestingAuthenticationToken(AUTHENTICATED_USER, "");
        studentId = randomUUID();
        creditId = randomUUID();
    }

    @Test
    void givenValidCreditRequestWhenEnablingCreditThenReturnsCreatedCredit() {
        givenAuthenticatedTutor();
        EnableSchoolCreditRequestDTO request = givenValidEnableCreditRequest("2000.00", 3);
        givenCreditIsEnabled("2000.00", 3);

        ResponseEntity<?> response = whenPostEnableCredit(request);

        thenCreditWasCreated(response, "2000.00", 3);
        thenEnableCreditWasRequestedWith(request, 3);
    }

    @Test
    void givenRequestWithoutInstallmentsWhenEnablingCreditThenUsesOneInstallment() {
        givenAuthenticatedTutor();
        EnableSchoolCreditRequestDTO request = givenValidEnableCreditRequest("1000.00", null);
        givenCreditIsEnabled("1000.00", 1);

        ResponseEntity<?> response = whenPostEnableCredit(request);

        thenCreditWasCreated(response, "1000.00", 1);
        thenEnableCreditWasRequestedWith(request, 1);
    }

    @Test
    void givenActiveCreditAlreadyExistsWhenEnablingCreditThenReturnsConflict() {
        givenAuthenticatedTutor();
        EnableSchoolCreditRequestDTO request = givenValidEnableCreditRequest("1000.00", 1);
        givenEnableCreditFailsWithActiveCredit();

        ResponseEntity<?> response = whenPostEnableCredit(request);

        thenResponseIsConflictWithMessage(response, "Already has active credit");
    }

    @Test
    void givenAuthenticatedUserWithoutTutorWhenEnablingCreditThenReturnsBadRequest() {
        givenAuthenticatedUserWithoutTutor();
        EnableSchoolCreditRequestDTO request = givenValidEnableCreditRequest("1000.00", 1);

        ResponseEntity<?> response = whenPostEnableCredit(request);

        thenResponseIsBadRequestWithMessage(response, "Tutor no encontrado para el usuario actual.");
        thenEnableCreditWasNotExecuted();
    }

    @Test
    void givenUseCaseRejectsCreditWhenEnablingCreditThenReturnsBadRequest() {
        givenAuthenticatedTutor();
        EnableSchoolCreditRequestDTO request = givenValidEnableCreditRequest("1000.00", 1);
        givenEnableCreditFailsWithIllegalArgument("Credit amount must be greater than zero");

        ResponseEntity<?> response = whenPostEnableCredit(request);

        thenResponseIsBadRequestWithMessage(response, "Credit amount must be greater than zero");
    }

    @Test
    void givenInvalidCreditRequestWhenEnablingCreditThenReturnsBadRequest() {
        givenAuthenticatedTutor();
        EnableSchoolCreditRequestDTO request = givenInvalidEnableCreditRequest();
        givenEnableCreditFailsWithIllegalArgument("Invalid request");
        
        ResponseEntity<?> response = whenPostEnableCredit(request);

        thenResponseIsBadRequestWithMessage(response, "Invalid request");
    }

    @Test
    void givenLastRechargeExistsWhenGettingLastRechargeThenReturnsAmount() {
        givenLastRechargeExists("1500.00");

        ResponseEntity<BigDecimal> response = whenGetLastRecharge();

        thenLastRechargeAmountWasReturned(response, "1500.00");
    }

    @Test
    void givenLastRechargeStudentDoesNotExistWhenGettingLastRechargeThenReturnsNotFound() {
        givenLastRechargeFailsWithIllegalArgument();

        ResponseEntity<BigDecimal> response = whenGetLastRecharge();

        thenResponseIsNotFound(response);
    }

    @Test
    void givenActiveCreditExistsWhenGettingActiveCreditThenReturnsCredit() {
        givenActiveCreditExists("2500.00", 2);

        ResponseEntity<GetActiveSchoolCreditResponse> response = whenGetActiveCredit();

        thenActiveCreditWasReturned(response, "2500.00", 2);
    }

    @Test
    void givenActiveCreditDoesNotExistWhenGettingActiveCreditThenReturnsEmptyBody() {
        givenActiveCreditDoesNotExist();

        ResponseEntity<GetActiveSchoolCreditResponse> response = whenGetActiveCredit();

        thenEmptyOkResponseWasReturned(response);
    }

    @Test
    void givenActiveCreditRequestIsInvalidWhenGettingActiveCreditThenReturnsBadRequest() {
        givenActiveCreditFailsWithIllegalArgument();

        ResponseEntity<GetActiveSchoolCreditResponse> response = whenGetActiveCredit();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void givenCreditCanBePaidWhenPayingCreditThenReturnsPaidCredit() {
        givenCreditCanBePaid("1200.00");

        ResponseEntity<?> response = whenPostPayCredit();

        thenPaidCreditWasReturned(response, "1200.00");
    }

    @Test
    void givenCreditCannotBePaidWhenPayingCreditThenReturnsBadRequest() {
        givenPayCreditFailsWithIllegalState("Credit already paid");

        ResponseEntity<?> response = whenPostPayCredit();

        thenResponseIsBadRequestWithMessage(response, "Credit already paid");
    }

    @Test
    void givenCreditHistoryExistsWhenGettingHistoryThenReturnsHistory() {
        givenCreditHistoryExists();

        ResponseEntity<List<SchoolCredit>> response = whenGetCreditHistory();

        thenCreditHistoryWasReturned(response);
    }

    @Test
    void givenCreditHistoryRequestIsInvalidWhenGettingHistoryThenReturnsBadRequest() {
        givenCreditHistoryFailsWithIllegalArgument();

        ResponseEntity<List<SchoolCredit>> response = whenGetCreditHistory();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    private void givenAuthenticatedTutor() {
        tutorId = randomUUID();
        Usuario user = new Usuario();
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        user.setTutor(tutor);

        when(usuarioService.obtenerUsuarioActual(AUTHENTICATED_USER)).thenReturn(user);
    }

    private void givenAuthenticatedUserWithoutTutor() {
        when(usuarioService.obtenerUsuarioActual(AUTHENTICATED_USER)).thenReturn(new Usuario());
    }

    private EnableSchoolCreditRequestDTO givenValidEnableCreditRequest(String amount, Integer installments) {
        EnableSchoolCreditRequestDTO request = new EnableSchoolCreditRequestDTO();
        request.setStudentId(studentId);
        request.setAmount(new BigDecimal(amount));
        request.setInstallments(installments);
        return request;
    }

    private EnableSchoolCreditRequestDTO givenInvalidEnableCreditRequest() {
        EnableSchoolCreditRequestDTO request = new EnableSchoolCreditRequestDTO();
        request.setStudentId(studentId);
        request.setAmount(BigDecimal.ZERO);
        return request;
    }

    private void givenCreditIsEnabled(String amount, int installments) {
        when(enableSchoolCredit.execute(any(EnableSchoolCreditRequest.class)))
                .thenReturn(new EnableSchoolCreditResponse(
                        creditId,
                        new BigDecimal(amount),
                        "ACTIVE",
                        installments
                ));
    }

    private void givenEnableCreditFailsWithActiveCredit() {
        when(enableSchoolCredit.execute(any(EnableSchoolCreditRequest.class)))
                .thenThrow(new ActiveCreditExistsException("Already has active credit"));
    }

    private void givenEnableCreditFailsWithIllegalArgument(String message) {
        when(enableSchoolCredit.execute(any(EnableSchoolCreditRequest.class)))
                .thenThrow(new IllegalArgumentException(message));
    }

    private void givenLastRechargeExists(String amount) {
        when(getLastRecharge.execute(any(GetLastRechargeRequest.class)))
                .thenReturn(new GetLastRechargeResponse(new BigDecimal(amount)));
    }

    private void givenLastRechargeFailsWithIllegalArgument() {
        when(getLastRecharge.execute(any(GetLastRechargeRequest.class)))
                .thenThrow(new IllegalArgumentException("Student not found"));
    }

    private void givenActiveCreditExists(String amount, int installments) {
        when(getActiveSchoolCredit.execute(any(GetActiveSchoolCreditRequest.class)))
                .thenReturn(Optional.of(new GetActiveSchoolCreditResponse(
                        creditId,
                        new BigDecimal(amount),
                        "ACTIVE",
                        installments
                )));
    }

    private void givenActiveCreditDoesNotExist() {
        when(getActiveSchoolCredit.execute(any(GetActiveSchoolCreditRequest.class)))
                .thenReturn(Optional.empty());
    }

    private void givenActiveCreditFailsWithIllegalArgument() {
        when(getActiveSchoolCredit.execute(any(GetActiveSchoolCreditRequest.class)))
                .thenThrow(new IllegalArgumentException("studentId cannot be null"));
    }

    private void givenCreditCanBePaid(String amount) {
        when(paySchoolCredit.execute(creditId))
                .thenReturn(schoolCredit(amount, CreditStatus.PAID, 1));
    }

    private void givenPayCreditFailsWithIllegalState(String message) {
        when(paySchoolCredit.execute(creditId))
                .thenThrow(new IllegalStateException(message));
    }

    private void givenCreditHistoryExists() {
        when(getSchoolCreditHistory.execute(studentId))
                .thenReturn(List.of(schoolCredit("900.00", CreditStatus.ACTIVE, 2)));
    }

    private void givenCreditHistoryFailsWithIllegalArgument() {
        when(getSchoolCreditHistory.execute(studentId))
                .thenThrow(new IllegalArgumentException("Student not found"));
    }

    private ResponseEntity<?> whenPostEnableCredit(EnableSchoolCreditRequestDTO request) {
        return controller.enableCredit(authentication, request);
    }

    private ResponseEntity<BigDecimal> whenGetLastRecharge() {
        return controller.getLastRechargeAmount(studentId);
    }

    private ResponseEntity<GetActiveSchoolCreditResponse> whenGetActiveCredit() {
        return controller.getActiveCredit(studentId);
    }

    private ResponseEntity<?> whenPostPayCredit() {
        return controller.payCredit(creditId);
    }

    private ResponseEntity<List<SchoolCredit>> whenGetCreditHistory() {
        return controller.getHistory(studentId);
    }

    private void thenCreditWasCreated(ResponseEntity<?> response, String amount, int installments) {
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        EnableSchoolCreditResponse body = (EnableSchoolCreditResponse) response.getBody();
        assertEquals(creditId, body.creditId());
        assertEquals(new BigDecimal(amount), body.amount());
        assertEquals(CreditStatus.ACTIVE.name(), body.status());
        assertEquals(installments, body.installments());
    }

    private void thenEnableCreditWasRequestedWith(EnableSchoolCreditRequestDTO requestDto, int installments) {
        ArgumentCaptor<EnableSchoolCreditRequest> captor = ArgumentCaptor.forClass(
                EnableSchoolCreditRequest.class);
        verify(enableSchoolCredit).execute(captor.capture());

        EnableSchoolCreditRequest request = captor.getValue();
        assertEquals(tutorId, request.tutorId());
        assertEquals(studentId, request.studentId());
        assertEquals(requestDto.getAmount(), request.amount());
        assertEquals(installments, request.installments());
    }

    private void thenLastRechargeAmountWasReturned(ResponseEntity<BigDecimal> response, String amount) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new BigDecimal(amount), response.getBody());
    }

    private void thenActiveCreditWasReturned(ResponseEntity<GetActiveSchoolCreditResponse> response, String amount, int installments) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(creditId, response.getBody().creditId());
        assertEquals(new BigDecimal(amount), response.getBody().amount());
        assertEquals(CreditStatus.ACTIVE.name(), response.getBody().status());
        assertEquals(installments, response.getBody().installments());
    }

    private void thenEmptyOkResponseWasReturned(ResponseEntity<GetActiveSchoolCreditResponse> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(null, response.getBody());
    }

    private void thenEnableCreditWasNotExecuted() {
        verify(enableSchoolCredit, never()).execute(any());
    }

    private void thenPaidCreditWasReturned(ResponseEntity<?> response, String amount) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        SchoolCredit body = (SchoolCredit) response.getBody();
        assertEquals(creditId, body.getId());
        assertEquals(studentId, body.getStudentId());
        assertEquals(new BigDecimal(amount), body.getAmount());
        assertEquals(CreditStatus.PAID, body.getStatus());
        assertEquals(1, body.getInstallments());
    }

    private void thenCreditHistoryWasReturned(ResponseEntity<List<SchoolCredit>> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(creditId, response.getBody().get(0).getId());
        assertEquals(studentId, response.getBody().get(0).getStudentId());
        assertEquals(new BigDecimal("900.00"), response.getBody().get(0).getAmount());
        assertEquals(CreditStatus.ACTIVE, response.getBody().get(0).getStatus());
        assertEquals(2, response.getBody().get(0).getInstallments());
    }

    private void thenResponseIsConflictWithMessage(ResponseEntity<?> response, String message) {
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(message, response.getBody());
    }

    private void thenResponseIsBadRequestWithMessage(ResponseEntity<?> response, String message) {
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(message, response.getBody());
    }

    private void thenResponseIsNotFound(ResponseEntity<?> response) {
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private SchoolCredit schoolCredit(String amount, CreditStatus status, int installments) {
        return new SchoolCredit(
                creditId,
                studentId,
                new BigDecimal(amount),
                status,
                installments,
                LocalDateTime.now()
        );
    }
}
