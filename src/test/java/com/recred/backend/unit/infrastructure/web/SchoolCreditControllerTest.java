package com.recred.backend.unit.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.recred.backend.config.GlobalExceptionHandler;
import com.recred.backend.domain.exceptions.ActiveCreditExistsException;
import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.SchoolCredit;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.UsuarioService;
import com.recred.backend.infrastructure.web.EnableSchoolCreditRequestDTO;
import com.recred.backend.infrastructure.web.SchoolCreditController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SchoolCreditControllerTest {

    private static final String AUTHENTICATED_USER = "test-user";
    private static final String SCHOOL_CREDITS_URL = "/api/v1/school-credits";

    private MockMvc mockMvc;

    @Mock private EnableSchoolCredit enableSchoolCredit;
    @Mock private GetLastRecharge getLastRecharge;
    @Mock private GetActiveSchoolCredit getActiveSchoolCredit;
    @Mock private PaySchoolCredit paySchoolCredit;
    @Mock private GetSchoolCreditHistory getSchoolCreditHistory;
    @Mock private UsuarioService usuarioService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ResultActions result;
    private EnableSchoolCreditRequestDTO enableCreditRequest;
    private UUID tutorId;
    private UUID studentId;
    private UUID creditId;

    @BeforeEach
    void setUp() {
        SchoolCreditController controller = new SchoolCreditController(
                enableSchoolCredit,
                paySchoolCredit,
                getSchoolCreditHistory,
                getLastRecharge,
                getActiveSchoolCredit,
                usuarioService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void givenValidCreditRequest_whenEnablingCredit_thenReturnsCreatedCredit() throws Exception {
        givenAuthenticatedTutor();
        givenValidEnableCreditRequest("2000.00", 3);
        givenCreditIsEnabled("2000.00", 3);

        whenPostEnableCredit();

        thenCreditWasCreated("2000.00", 3);
        thenEnableCreditWasRequestedWith(3);
    }

    @Test
    void givenRequestWithoutInstallments_whenEnablingCredit_thenUsesOneInstallment()
            throws Exception {
        givenAuthenticatedTutor();
        givenValidEnableCreditRequestWithoutInstallments("1000.00");
        givenCreditIsEnabled("1000.00", 1);

        whenPostEnableCredit();

        thenCreditWasCreated("1000.00", 1);
        thenEnableCreditWasRequestedWith(1);
    }

    @Test
    void givenActiveCreditAlreadyExists_whenEnablingCredit_thenReturnsConflict()
            throws Exception {
        givenAuthenticatedTutor();
        givenValidEnableCreditRequest("1000.00", 1);
        givenEnableCreditFailsWithActiveCredit();

        whenPostEnableCredit();

        thenConflictWasReturned();
    }

    @Test
    void givenAuthenticatedUserWithoutTutor_whenEnablingCredit_thenReturnsBadRequest()
            throws Exception {
        givenAuthenticatedUserWithoutTutor();
        givenValidEnableCreditRequest("1000.00", 1);

        whenPostEnableCredit();

        thenBadRequestWasReturned("Tutor no encontrado para el usuario actual.");
        thenEnableCreditWasNotExecuted();
    }

    @Test
    void givenUseCaseRejectsCredit_whenEnablingCredit_thenReturnsBadRequest() throws Exception {
        givenAuthenticatedTutor();
        givenValidEnableCreditRequest("1000.00", 1);
        givenEnableCreditFailsWithIllegalArgument("Credit amount must be greater than zero");

        whenPostEnableCredit();

        thenBadRequestWasReturned("Credit amount must be greater than zero");
    }

    @Test
    void givenUnexpectedError_whenEnablingCredit_thenReturnsInternalServerError()
            throws Exception {
        givenValidEnableCreditRequest("1000.00", 1);
        givenCurrentUserFailsUnexpectedly();

        whenPostEnableCredit();

        thenInternalServerErrorWasReturned("An unexpected error occurred.");
    }

    @Test
    void givenInvalidCreditRequest_whenEnablingCredit_thenReturnsBadRequestBeforeUseCase()
            throws Exception {
        givenInvalidEnableCreditRequest();

        whenPostEnableCredit();

        thenBadRequestWasReturned();
        thenEnableCreditWasNotExecuted();
    }

    @Test
    void givenLastRechargeExists_whenGettingLastRecharge_thenReturnsAmount() throws Exception {
        givenLastRechargeExists("1500.00");

        whenGetLastRecharge();

        thenLastRechargeAmountWasReturned("1500.00");
    }

    @Test
    void givenLastRechargeStudentDoesNotExist_whenGettingLastRecharge_thenReturnsNotFound()
            throws Exception {
        givenLastRechargeFailsWithIllegalArgument();

        whenGetLastRecharge();

        thenNotFoundWasReturned();
    }

    @Test
    void givenLastRechargeFailsUnexpectedly_whenGettingLastRecharge_thenReturnsInternalServerError()
            throws Exception {
        givenLastRechargeFailsUnexpectedly();

        whenGetLastRecharge();

        thenInternalServerErrorWasReturned();
    }

    @Test
    void givenActiveCreditExists_whenGettingActiveCredit_thenReturnsCredit() throws Exception {
        givenActiveCreditExists("2500.00", 2);

        whenGetActiveCredit();

        thenActiveCreditWasReturned("2500.00", 2);
    }

    @Test
    void givenActiveCreditDoesNotExist_whenGettingActiveCredit_thenReturnsEmptyBody()
            throws Exception {
        givenActiveCreditDoesNotExist();

        whenGetActiveCredit();

        thenEmptyOkResponseWasReturned();
    }

    @Test
    void givenActiveCreditRequestIsInvalid_whenGettingActiveCredit_thenReturnsBadRequest()
            throws Exception {
        givenActiveCreditFailsWithIllegalArgument();

        whenGetActiveCredit();

        thenBadRequestWasReturned();
    }

    @Test
    void givenActiveCreditFailsUnexpectedly_whenGettingActiveCredit_thenReturnsInternalServerError()
            throws Exception {
        givenActiveCreditFailsUnexpectedly();

        whenGetActiveCredit();

        thenInternalServerErrorWasReturned();
    }

    @Test
    void givenCreditCanBePaid_whenPayingCredit_thenReturnsPaidCredit() throws Exception {
        givenCreditCanBePaid("1200.00");

        whenPostPayCredit();

        thenPaidCreditWasReturned("1200.00");
    }

    @Test
    void givenCreditCannotBePaid_whenPayingCredit_thenReturnsBadRequest() throws Exception {
        givenPayCreditFailsWithIllegalState("Credit already paid");

        whenPostPayCredit();

        thenBadRequestWasReturned("Credit already paid");
    }

    @Test
    void givenPayCreditFailsUnexpectedly_whenPayingCredit_thenReturnsInternalServerError()
            throws Exception {
        givenPayCreditFailsUnexpectedly();

        whenPostPayCredit();

        thenInternalServerErrorWasReturned(
                "An unexpected error occurred while paying the credit.");
    }

    @Test
    void givenCreditHistoryExists_whenGettingHistory_thenReturnsHistory() throws Exception {
        givenCreditHistoryExists();

        whenGetCreditHistory();

        thenCreditHistoryWasReturned();
    }

    @Test
    void givenCreditHistoryRequestIsInvalid_whenGettingHistory_thenReturnsBadRequest()
            throws Exception {
        givenCreditHistoryFailsWithIllegalArgument();

        whenGetCreditHistory();

        thenBadRequestWasReturned();
    }

    @Test
    void givenCreditHistoryFailsUnexpectedly_whenGettingHistory_thenReturnsInternalServerError()
            throws Exception {
        givenCreditHistoryFailsUnexpectedly();

        whenGetCreditHistory();

        thenInternalServerErrorWasReturned();
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

    private void givenCurrentUserFailsUnexpectedly() {
        when(usuarioService.obtenerUsuarioActual(AUTHENTICATED_USER))
                .thenThrow(new RuntimeException("boom"));
    }

    private void givenValidEnableCreditRequest(String amount, Integer installments) {
        studentId = randomUUID();
        enableCreditRequest = new EnableSchoolCreditRequestDTO();
        enableCreditRequest.setStudentId(studentId);
        enableCreditRequest.setAmount(new BigDecimal(amount));
        enableCreditRequest.setInstallments(installments);
    }

    private void givenValidEnableCreditRequestWithoutInstallments(String amount) {
        givenValidEnableCreditRequest(amount, null);
    }

    private void givenInvalidEnableCreditRequest() {
        studentId = randomUUID();
        enableCreditRequest = new EnableSchoolCreditRequestDTO();
        enableCreditRequest.setStudentId(studentId);
        enableCreditRequest.setAmount(BigDecimal.ZERO);
    }

    private void givenCreditIsEnabled(String amount, int installments) {
        creditId = randomUUID();
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

    private void givenLastRechargeFailsUnexpectedly() {
        when(getLastRecharge.execute(any(GetLastRechargeRequest.class)))
                .thenThrow(new RuntimeException("boom"));
    }

    private void givenActiveCreditExists(String amount, int installments) {
        creditId = randomUUID();
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

    private void givenActiveCreditFailsUnexpectedly() {
        when(getActiveSchoolCredit.execute(any(GetActiveSchoolCreditRequest.class)))
                .thenThrow(new RuntimeException("boom"));
    }

    private void givenCreditCanBePaid(String amount) {
        creditId = randomUUID();
        studentId = randomUUID();
        when(paySchoolCredit.execute(creditId))
                .thenReturn(schoolCredit(amount, CreditStatus.PAID, 1));
    }

    private void givenPayCreditFailsWithIllegalState(String message) {
        creditId = randomUUID();
        when(paySchoolCredit.execute(creditId))
                .thenThrow(new IllegalStateException(message));
    }

    private void givenPayCreditFailsUnexpectedly() {
        creditId = randomUUID();
        when(paySchoolCredit.execute(creditId))
                .thenThrow(new RuntimeException("boom"));
    }

    private void givenCreditHistoryExists() {
        studentId = randomUUID();
        creditId = randomUUID();
        when(getSchoolCreditHistory.execute(studentId))
                .thenReturn(List.of(schoolCredit("900.00", CreditStatus.ACTIVE, 2)));
    }

    private void givenCreditHistoryFailsWithIllegalArgument() {
        studentId = randomUUID();
        when(getSchoolCreditHistory.execute(studentId))
                .thenThrow(new IllegalArgumentException("Student not found"));
    }

    private void givenCreditHistoryFailsUnexpectedly() {
        studentId = randomUUID();
        when(getSchoolCreditHistory.execute(studentId))
                .thenThrow(new RuntimeException("boom"));
    }

    private void whenPostEnableCredit() throws Exception {
        result = mockMvc.perform(post(SCHOOL_CREDITS_URL)
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(enableCreditRequest)));
    }

    private void whenGetLastRecharge() throws Exception {
        studentId = randomUUID();
        result = mockMvc.perform(get(
                "/api/v1/school-credits/alumno/{studentId}/last-recharge",
                studentId
        ));
    }

    private void whenGetActiveCredit() throws Exception {
        studentId = randomUUID();
        result = mockMvc.perform(get(
                "/api/v1/school-credits/alumno/{studentId}/active",
                studentId
        ));
    }

    private void whenPostPayCredit() throws Exception {
        result = mockMvc.perform(post(
                "/api/v1/school-credits/{creditId}/pay",
                creditId
        ));
    }

    private void whenGetCreditHistory() throws Exception {
        result = mockMvc.perform(get(
                "/api/v1/school-credits/alumno/{studentId}",
                studentId
        ));
    }

    private void thenCreditWasCreated(String amount, int installments) throws Exception {
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.creditId").value(creditId.toString()))
                .andExpect(jsonPath("$.amount").value(Double.parseDouble(amount)))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.installments").value(installments));
    }

    private void thenEnableCreditWasRequestedWith(int installments) {
        ArgumentCaptor<EnableSchoolCreditRequest> captor = ArgumentCaptor.forClass(
                EnableSchoolCreditRequest.class);
        verify(enableSchoolCredit).execute(captor.capture());

        EnableSchoolCreditRequest request = captor.getValue();
        assertEquals(tutorId, request.tutorId());
        assertEquals(studentId, request.studentId());
        assertEquals(enableCreditRequest.getAmount(), request.amount());
        assertEquals(installments, request.installments());
    }

    private void thenConflictWasReturned() throws Exception {
        result.andExpect(status().isConflict());
    }

    private void thenBadRequestWasReturned(String message) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(content().string(message));
    }

    private void thenBadRequestWasReturned() throws Exception {
        result.andExpect(status().isBadRequest());
    }

    private void thenInternalServerErrorWasReturned(String message) throws Exception {
        result.andExpect(status().isInternalServerError())
                .andExpect(content().string(message));
    }

    private void thenInternalServerErrorWasReturned() throws Exception {
        result.andExpect(status().isInternalServerError());
    }

    private void thenNotFoundWasReturned() throws Exception {
        result.andExpect(status().isNotFound());
    }

    private void thenLastRechargeAmountWasReturned(String amount) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(content().string(amount));
    }

    private void thenActiveCreditWasReturned(String amount, int installments) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.creditId").value(creditId.toString()))
                .andExpect(jsonPath("$.amount").value(Double.parseDouble(amount)))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.installments").value(installments));
    }

    private void thenEmptyOkResponseWasReturned() throws Exception {
        result.andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    private void thenEnableCreditWasNotExecuted() {
        verify(enableSchoolCredit, never()).execute(any());
    }

    private void thenPaidCreditWasReturned(String amount) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(creditId.toString()))
                .andExpect(jsonPath("$.studentId").value(studentId.toString()))
                .andExpect(jsonPath("$.amount").value(Double.parseDouble(amount)))
                .andExpect(jsonPath("$.status").value(CreditStatus.PAID.name()))
                .andExpect(jsonPath("$.installments").value(1));
    }

    private void thenCreditHistoryWasReturned() throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(creditId.toString()))
                .andExpect(jsonPath("$[0].studentId").value(studentId.toString()))
                .andExpect(jsonPath("$[0].amount").value(900.0))
                .andExpect(jsonPath("$[0].status").value(CreditStatus.ACTIVE.name()))
                .andExpect(jsonPath("$[0].installments").value(2));
    }

    private Authentication authentication() {
        return new UsernamePasswordAuthenticationToken(AUTHENTICATED_USER, "password");
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
