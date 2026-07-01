package com.recred.backend.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.ApproveSchoolRegistration;
import com.recred.backend.application.RejectSchoolRegistration;
import com.recred.backend.application.SubmitSchoolRegistration;
import com.recred.backend.config.CustomJwtAuthenticationConverter;
import com.recred.backend.domain.models.SchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationStatus;
import com.recred.backend.domain.repositories.SchoolRegistrationRequestRepository;
import com.recred.backend.infrastructure.web.SchoolRegistrationController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
    private SchoolRegistrationRequestRepository repository;

    private UUID requestId;
    private TestingAuthenticationToken adminAuth;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        adminAuth = new TestingAuthenticationToken("admin-123", null);
    }

    @Test
    void submit_ShouldReturn200() throws Exception {
        String requestJson = """
                {
                    "schoolName": "Test School",
                    "schoolEmail": "school@test.com",
                    "schoolPhone": "123456789",
                    "schoolCue": "12345",
                    "directorFirstName": "John",
                    "directorLastName": "Doe",
                    "directorEmail": "john@test.com"
                }
                """;

        mockMvc.perform(post("/api/v1/school-registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        verify(submitUseCase).execute(any());
    }

    @Test
    void approve_ShouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/school-registrations/" + requestId + "/approve")
                        .principal(adminAuth))
                .andExpect(status().isOk());

        verify(approveUseCase).execute(any());
    }

    @Test
    void reject_ShouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/school-registrations/" + requestId + "/reject")
                        .principal(adminAuth))
                .andExpect(status().isOk());

        verify(rejectUseCase).execute(any());
    }

    @Test
    void getByStatus_ShouldReturnList() throws Exception {
        SchoolRegistrationRequest mockRequest = new SchoolRegistrationRequest();
        mockRequest.setId(requestId);
        mockRequest.setSchoolName("Test School");

        when(repository.findByStatus(SchoolRegistrationStatus.PENDING))
                .thenReturn(Collections.singletonList(mockRequest));

        mockMvc.perform(get("/api/v1/school-registrations")
                        .param("status", "PENDING")
                        .principal(adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requestId.toString()))
                .andExpect(jsonPath("$[0].schoolName").value("Test School"));
    }
}
