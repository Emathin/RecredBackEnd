package com.recred.backend.web;

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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    private UsuarioService usuarioService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void enableCredit_success() throws Exception {
        EnableSchoolCreditRequest request = new EnableSchoolCreditRequest();
        request.setStudentId(UUID.randomUUID());
        request.setAmount(new BigDecimal("2000.00"));
        request.setInstallments(3);

        SchoolCredit credit = new SchoolCredit();
        credit.setId(UUID.randomUUID());
        credit.setStudentId(request.getStudentId());
        credit.setAmount(new BigDecimal("2000.00"));
        credit.setStatus(CreditStatus.ACTIVE);
        credit.setInstallments(3);
        credit.setCreatedAt(LocalDateTime.now());

        Usuario mockUsuario = new Usuario();
        Tutor mockTutor = new Tutor();
        mockTutor.setId(UUID.randomUUID());
        mockUsuario.setTutor(mockTutor);

        when(usuarioService.obtenerUsuarioActual("test-user")).thenReturn(mockUsuario);
        when(enableSchoolCredit.execute(any(UUID.class), any(UUID.class), any(BigDecimal.class), any(Integer.class)))
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
        when(enableSchoolCredit.execute(any(UUID.class), any(UUID.class), any(), any()))
                .thenThrow(new ActiveCreditExistsException("Already has active credit"));

        Authentication authentication = new UsernamePasswordAuthenticationToken("test-user", "password");

        mockMvc.perform(post("/api/v1/school-credits")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
