package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.ApproveSchoolRegistration;
import com.recred.backend.application.ApproveSchoolRegistrationRequest;
import com.recred.backend.application.GetSchoolRegistrations;
import com.recred.backend.application.GetSchoolRegistrationsResponse;
import com.recred.backend.application.RejectSchoolRegistration;
import com.recred.backend.application.RejectSchoolRegistrationRequest;
import com.recred.backend.application.SubmitSchoolRegistration;
import com.recred.backend.application.SubmitSchoolRegistrationRequest;
import com.recred.backend.config.CustomJwtAuthenticationConverter;
import com.recred.backend.infrastructure.web.AdminAccessGuard;
import com.recred.backend.infrastructure.web.SchoolRegistrationController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SchoolRegistrationController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class SchoolRegistrationControllerTest {

    private static final String SCHOOL_NAME = "Test School";
    private static final String SCHOOL_EMAIL = "school@test.com";
    private static final String SCHOOL_PHONE = "123456789";
    private static final String SCHOOL_CUE = "123456789";
    private static final String DIRECTOR_FIRST_NAME = "John";
    private static final String DIRECTOR_LAST_NAME = "Doe";
    private static final String DIRECTOR_EMAIL = "john@test.com";
    private static final String DIRECTOR_PHONE = "987654321";
    private static final String DIRECTOR_DNI = "12345678";
    private static final String DIRECTOR_USERNAME = "john-doe";
    private static final String PENDING_STATUS = "PENDING";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    @MockitoBean
    private SubmitSchoolRegistration submitUseCase;

    @MockitoBean
    private ApproveSchoolRegistration approveUseCase;

    @MockitoBean
    private RejectSchoolRegistration rejectUseCase;

    @MockitoBean
    private GetSchoolRegistrations getSchoolRegistrationsUseCase;

    @MockitoBean
    private AdminAccessGuard adminAccessGuard;

    private UUID registrationId;
    private TestingAuthenticationToken adminAuthentication;
    private String submissionJson;
    private ResultActions response;

    @BeforeEach
    void setUp() {
        adminAuthentication = new TestingAuthenticationToken("admin-123", null);
    }

    @Test
    void givenSchoolRegistrationSubmission_whenSubmittingRegistration_thenDelegatesToUseCase()
            throws Exception {
        givenSchoolRegistrationSubmission();

        whenSubmitSchoolRegistration();

        thenResponseIsOk();
        thenSubmitRegistrationWasRequested();
    }

    @Test
    void givenInvalidSchoolRegistrationSubmission_whenSubmittingRegistration_thenReturnsBadRequest()
            throws Exception {
        givenInvalidSchoolRegistrationSubmission();

        whenSubmitSchoolRegistration();

        thenResponseIsBadRequest();
    }

    @Test
    void givenAdminUser_whenApprovingRegistration_thenDelegatesToUseCase() throws Exception {
        givenRegistrationId();

        whenApproveSchoolRegistration();

        thenResponseIsOk();
        thenAdminAccessWasRequired();
        thenApproveRegistrationWasRequested();
    }

    @Test
    void givenAdminUser_whenRejectingRegistration_thenDelegatesToUseCase() throws Exception {
        givenRegistrationId();

        whenRejectSchoolRegistration();

        thenResponseIsOk();
        thenAdminAccessWasRequired();
        thenRejectRegistrationWasRequested();
    }

    @Test
    void givenRegistrationsWithStatus_whenGettingRegistrations_thenReturnsRegistrationList()
            throws Exception {
        givenRegistrationId();
        givenRegistrationsWithStatus(PENDING_STATUS);

        whenGetRegistrationsByStatus(PENDING_STATUS);

        thenResponseIsOk();
        thenAdminAccessWasRequired();
        thenRegistrationsAreReturned();
    }

    private void givenSchoolRegistrationSubmission() {
        submissionJson = """
                {
                    "schoolName": "%s",
                    "schoolEmail": "%s",
                    "schoolPhone": "%s",
                    "schoolCue": "%s",
                    "directorFirstName": "%s",
                    "directorLastName": "%s",
                    "directorEmail": "%s",
                    "directorPhone": "%s",
                    "directorDni": "%s",
                    "directorUsername": "%s"
                }
                """.formatted(
                SCHOOL_NAME,
                SCHOOL_EMAIL,
                SCHOOL_PHONE,
                SCHOOL_CUE,
                DIRECTOR_FIRST_NAME,
                DIRECTOR_LAST_NAME,
                DIRECTOR_EMAIL,
                DIRECTOR_PHONE,
                DIRECTOR_DNI,
                DIRECTOR_USERNAME);
    }

    private void givenInvalidSchoolRegistrationSubmission() {
        submissionJson = """
                {
                    "schoolName": "",
                    "schoolEmail": "not-an-email",
                    "schoolPhone": "",
                    "schoolCue": "123",
                    "directorFirstName": "",
                    "directorLastName": "",
                    "directorEmail": "",
                    "directorPhone": "",
                    "directorDni": "123",
                    "directorUsername": ""
                }
                """;
    }

    private void givenRegistrationId() {
        registrationId = randomUUID();
    }

    private void givenRegistrationsWithStatus(String status) {
        GetSchoolRegistrationsResponse registration = new GetSchoolRegistrationsResponse(
                registrationId,
                SCHOOL_NAME,
                SCHOOL_EMAIL,
                SCHOOL_PHONE,
                SCHOOL_CUE,
                DIRECTOR_FIRST_NAME,
                DIRECTOR_LAST_NAME,
                DIRECTOR_EMAIL,
                DIRECTOR_PHONE,
                DIRECTOR_DNI,
                DIRECTOR_USERNAME,
                status,
                null
        );
        when(getSchoolRegistrationsUseCase.execute(status)).thenReturn(List.of(registration));
    }

    private void whenSubmitSchoolRegistration() throws Exception {
        response = mockMvc.perform(post("/api/v1/school-registrations")
                .contentType(APPLICATION_JSON)
                .content(submissionJson));
    }

    private void whenApproveSchoolRegistration() throws Exception {
        response = mockMvc.perform(post("/api/v1/school-registrations/{id}/approve",
                registrationId).principal(adminAuthentication));
    }

    private void whenRejectSchoolRegistration() throws Exception {
        response = mockMvc.perform(post("/api/v1/school-registrations/{id}/reject",
                registrationId).principal(adminAuthentication));
    }

    private void whenGetRegistrationsByStatus(String status) throws Exception {
        response = mockMvc.perform(get("/api/v1/school-registrations")
                .param("status", status)
                .principal(adminAuthentication));
    }

    private void thenResponseIsOk() throws Exception {
        response.andExpect(status().isOk());
    }

    private void thenResponseIsBadRequest() throws Exception {
        response.andExpect(status().isBadRequest());
    }

    private void thenSubmitRegistrationWasRequested() {
        verify(submitUseCase).execute(new SubmitSchoolRegistrationRequest(
                SCHOOL_NAME,
                SCHOOL_EMAIL,
                SCHOOL_PHONE,
                SCHOOL_CUE,
                DIRECTOR_FIRST_NAME,
                DIRECTOR_LAST_NAME,
                DIRECTOR_EMAIL,
                DIRECTOR_PHONE,
                DIRECTOR_DNI,
                DIRECTOR_USERNAME
        ));
    }

    private void thenAdminAccessWasRequired() {
        verify(adminAccessGuard).requireAdminAccess(adminAuthentication);
    }

    private void thenApproveRegistrationWasRequested() {
        verify(approveUseCase).execute(new ApproveSchoolRegistrationRequest(registrationId));
    }

    private void thenRejectRegistrationWasRequested() {
        verify(rejectUseCase).execute(new RejectSchoolRegistrationRequest(registrationId));
    }

    private void thenRegistrationsAreReturned() throws Exception {
        response.andExpect(jsonPath("$[0].id").value(registrationId.toString()))
                .andExpect(jsonPath("$[0].schoolName").value(SCHOOL_NAME))
                .andExpect(jsonPath("$[0].status").value(PENDING_STATUS));
    }
}
