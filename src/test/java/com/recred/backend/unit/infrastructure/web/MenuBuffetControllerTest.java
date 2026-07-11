package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.GenerateBuffetMenu;
import com.recred.backend.application.GetBuffetByStudent;
import com.recred.backend.application.GetBuffetByStudentRequest;
import com.recred.backend.application.GetBuffetByStudentResponse;
import com.recred.backend.config.CustomAuthenticationEntryPoint;
import com.recred.backend.config.CustomJwtAuthenticationConverter;
import com.recred.backend.infrastructure.web.MenuBuffetController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MenuBuffetController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class}
)
class MenuBuffetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GenerateBuffetMenu generateBuffetMenu;

    @MockitoBean
    private GetBuffetByStudent getBuffetByStudent;

    @MockitoBean
    private CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final UUID ALUMNO_ID = UUID.randomUUID();
    private final UUID BUFFET_ID = UUID.randomUUID();
    private final UUID COLEGIO_ID = UUID.randomUUID();

    @Test
    void getBuffetGivenStudentWithBuffetShouldReturnBuffetDTOAnd200() throws Exception {
        givenStudentHasBuffet();
        
        ResultActions response = whenGettingBuffet();
        
        thenBuffetIsReturned(response);
    }

    @Test
    void getBuffetGivenStudentWithoutBuffetShouldReturn404() throws Exception {
        givenStudentHasNoBuffet();
        
        ResultActions response = whenGettingBuffet();
        
        thenStatusIsNotFound(response);
    }

    private void givenStudentHasBuffet() {
        GetBuffetByStudentResponse response = new GetBuffetByStudentResponse(
                BUFFET_ID,
                "Buffet del Colegio",
                COLEGIO_ID,
                null,
                null,
                true,
                null,
                null
        );
        when(getBuffetByStudent.execute(new GetBuffetByStudentRequest(ALUMNO_ID)))
                .thenReturn(Optional.of(response));
    }

    private void givenStudentHasNoBuffet() {
        when(getBuffetByStudent.execute(new GetBuffetByStudentRequest(ALUMNO_ID)))
                .thenReturn(Optional.empty());
    }

    private ResultActions whenGettingBuffet() throws Exception {
        return mockMvc.perform(get("/api/v1/alumnos/" + ALUMNO_ID + "/buffet")
                .contentType(MediaType.APPLICATION_JSON));
    }

    private void thenBuffetIsReturned(ResultActions response) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(BUFFET_ID.toString()))
                .andExpect(jsonPath("$.nombre").value("Buffet del Colegio"))
                .andExpect(jsonPath("$.colegioId").value(COLEGIO_ID.toString()));
    }

    private void thenStatusIsNotFound(ResultActions response) throws Exception {
        response.andExpect(status().isNotFound());
    }
}
