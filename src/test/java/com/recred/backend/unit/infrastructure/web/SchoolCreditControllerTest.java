package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.infrastructure.web.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.EnableSchoolCredit;
import com.recred.backend.application.GetActiveSchoolCredit;
import com.recred.backend.application.GetLastRecharge;
import com.recred.backend.domain.exceptions.ActiveCreditExistsException;
import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.SchoolCredit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.recred.backend.domain.services.UsuarioService;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SchoolCreditController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
public class SchoolCreditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnableSchoolCredit enableSchoolCredit;

    @MockitoBean
    private GetLastRecharge getLastRecharge;

    @MockitoBean
    private GetActiveSchoolCredit getActiveSchoolCredit;

    @MockitoBean
    private com.recred.backend.application.PaySchoolCredit paySchoolCredit;

    @MockitoBean
    private com.recred.backend.application.GetSchoolCreditHistory getSchoolCreditHistory;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private SchoolLicenseAccessGuard schoolLicenseAccessGuard;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void enableCredit_success() throws Exception {
        EnableSchoolCreditRequest request = new EnableSchoolCreditRequest();
        request.setStudentId(UUID.randomUUID());
        request.setAmount(new BigDecimal("2000.00"));
        request.setInstallments(3);

        com.recred.backend.application.EnableSchoolCreditResponse credit = new com.recred.backend.application.EnableSchoolCreditResponse(UUID.randomUUID(), request.getAmount(), "ACTIVE", 3);
        
        
        
        
        
        

        Usuario mockUsuario = new Usuario();
        Tutor mockTutor = new Tutor();
        mockTutor.setId(UUID.randomUUID());
        mockUsuario.setTutor(mockTutor);

        when(usuarioService.obtenerUsuarioActual("test-user")).thenReturn(mockUsuario);
        when(enableSchoolCredit.execute(any(com.recred.backend.application.EnableSchoolCreditRequest.class)))
                .thenReturn(credit);

        Authentication authentication = new UsernamePasswordAuthenticationToken("test-user", "password");

        mockMvc.perform(post("/api/v1/school-credits")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(2000.0))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.installments").value(3));
    }

    @Test
    void enableCredit_conflict() throws Exception {
        EnableSchoolCreditRequest request = new EnableSchoolCreditRequest();
        request.setParentId(UUID.randomUUID());
        request.setStudentId(UUID.randomUUID());

        Usuario mockUsuario = new Usuario();
        Tutor mockTutor = new Tutor();
        mockTutor.setId(UUID.randomUUID());
        mockUsuario.setTutor(mockTutor);

        when(usuarioService.obtenerUsuarioActual("test-user")).thenReturn(mockUsuario);
        when(enableSchoolCredit.execute(any(com.recred.backend.application.EnableSchoolCreditRequest.class)))
                .thenThrow(new ActiveCreditExistsException("Already has active credit"));

        Authentication authentication = new UsernamePasswordAuthenticationToken("test-user", "password");

        mockMvc.perform(post("/api/v1/school-credits")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void enableCredit_withoutTutor_ShouldReturnBadRequest() throws Exception {
        EnableSchoolCreditRequest request = new EnableSchoolCreditRequest();
        request.setStudentId(UUID.randomUUID());
        request.setAmount(new BigDecimal("1000.00"));

        when(usuarioService.obtenerUsuarioActual("test-user")).thenReturn(new Usuario());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "test-user",
                "password");

        mockMvc.perform(post("/api/v1/school-credits")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Tutor no encontrado para el usuario actual."));
    }

    @Test
    void enableCredit_illegalArgument_ShouldReturnBadRequest() throws Exception {
        EnableSchoolCreditRequest request = new EnableSchoolCreditRequest();
        request.setStudentId(UUID.randomUUID());
        request.setAmount(new BigDecimal("1000.00"));
        Usuario mockUsuario = new Usuario();
        Tutor mockTutor = new Tutor();
        mockTutor.setId(UUID.randomUUID());
        mockUsuario.setTutor(mockTutor);
        when(usuarioService.obtenerUsuarioActual("test-user")).thenReturn(mockUsuario);
        when(enableSchoolCredit.execute(any(com.recred.backend.application
                .EnableSchoolCreditRequest.class)))
                .thenThrow(new IllegalArgumentException("Credit amount must be greater than zero"));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "test-user",
                "password");

        mockMvc.perform(post("/api/v1/school-credits")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Credit amount must be greater than zero"));
    }

    @Test
    void enableCredit_unexpectedError_ShouldReturnInternalServerError() throws Exception {
        EnableSchoolCreditRequest request = new EnableSchoolCreditRequest();
        request.setStudentId(UUID.randomUUID());
        request.setAmount(new BigDecimal("1000.00"));
        when(usuarioService.obtenerUsuarioActual("test-user"))
                .thenThrow(new RuntimeException("boom"));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "test-user",
                "password");

        mockMvc.perform(post("/api/v1/school-credits")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("An unexpected error occurred."));
    }

    @Test
    void getLastRecharge_successAndErrors() throws Exception {
        UUID studentId = UUID.randomUUID();
        when(getLastRecharge.execute(any(com.recred.backend.application
                .GetLastRechargeRequest.class)))
                .thenReturn(new com.recred.backend.application.GetLastRechargeResponse(
                        new BigDecimal("1500.00")))
                .thenThrow(new IllegalArgumentException("Student not found"))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v1/school-credits/alumno/{studentId}/last-recharge",
                studentId))
                .andExpect(status().isOk())
                .andExpect(content().string("1500.00"));
        mockMvc.perform(get("/api/v1/school-credits/alumno/{studentId}/last-recharge",
                studentId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/school-credits/alumno/{studentId}/last-recharge",
                studentId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getActiveCredit_successEmptyAndErrors() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID creditId = UUID.randomUUID();
        when(getActiveSchoolCredit.execute(any(com.recred.backend.application
                .GetActiveSchoolCreditRequest.class)))
                .thenReturn(Optional.of(new com.recred.backend.application
                        .GetActiveSchoolCreditResponse(
                        creditId,
                        new BigDecimal("2500.00"),
                        "ACTIVE",
                        2)))
                .thenReturn(Optional.empty())
                .thenThrow(new IllegalArgumentException("studentId cannot be null"))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v1/school-credits/alumno/{studentId}/active",
                studentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditId").value(creditId.toString()))
                .andExpect(jsonPath("$.amount").value(2500.0))
                .andExpect(jsonPath("$.installments").value(2));
        mockMvc.perform(get("/api/v1/school-credits/alumno/{studentId}/active",
                studentId))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
        mockMvc.perform(get("/api/v1/school-credits/alumno/{studentId}/active",
                studentId))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/school-credits/alumno/{studentId}/active",
                studentId))
                .andExpect(status().isInternalServerError());
    }
}
