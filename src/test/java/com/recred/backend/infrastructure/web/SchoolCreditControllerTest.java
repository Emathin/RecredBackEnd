package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.EnableSchoolCredit;
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

@WebMvcTest(
    controllers = SchoolCreditController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
    }
)
public class SchoolCreditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnableSchoolCredit enableSchoolCredit;

    @MockitoBean
    private UsuarioService usuarioService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void enableCredit_success() throws Exception {
        EnableSchoolCreditRequest request = new EnableSchoolCreditRequest();
        request.setParentId(UUID.randomUUID());
        request.setStudentId(UUID.randomUUID());

        SchoolCredit credit = new SchoolCredit();
        credit.setId(UUID.randomUUID());
        credit.setStudentId(request.getStudentId());
        credit.setAmount(BigDecimal.valueOf(55.0));
        credit.setStatus(CreditStatus.ACTIVE);
        credit.setCreatedAt(LocalDateTime.now());

        Usuario mockUsuario = new Usuario();
        Tutor mockTutor = new Tutor();
        mockTutor.setId(UUID.randomUUID());
        mockUsuario.setTutor(mockTutor);

        when(usuarioService.obtenerUsuarioActual("test-user")).thenReturn(mockUsuario);
        when(enableSchoolCredit.execute(any(UUID.class), any(UUID.class))).thenReturn(credit);

        Authentication authentication = new UsernamePasswordAuthenticationToken("test-user", "password");

        mockMvc.perform(post("/api/v1/school-credits")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(55.0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
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
        when(enableSchoolCredit.execute(any(UUID.class), any(UUID.class)))
                .thenThrow(new ActiveCreditExistsException("Already has active credit"));

        Authentication authentication = new UsernamePasswordAuthenticationToken("test-user", "password");

        mockMvc.perform(post("/api/v1/school-credits")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
