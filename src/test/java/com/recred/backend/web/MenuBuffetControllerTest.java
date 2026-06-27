package com.recred.backend.web;

import com.recred.backend.application.GenerarMenuBuffet;
import com.recred.backend.application.GetBuffetByAlumno;
import com.recred.backend.application.GetBuffetByAlumnoRequest;
import com.recred.backend.application.GetBuffetByAlumnoResponse;
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
    private GenerarMenuBuffet generarMenuBuffet;

    @MockitoBean
    private GetBuffetByAlumno getBuffetByAlumno;

    @MockitoBean
    private CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void obtenerBuffet_dadoAlumnoConBuffet_deberiaRetornarBuffetDTOy200() throws Exception {
        UUID alumnoId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UUID colegioId = UUID.randomUUID();

        GetBuffetByAlumnoResponse response = new GetBuffetByAlumnoResponse(
                buffetId,
                "Buffet del Colegio",
                colegioId,
                null,
                null,
                true,
                null,
                null
        );

        when(getBuffetByAlumno.execute(new GetBuffetByAlumnoRequest(alumnoId))).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/v1/alumnos/" + alumnoId + "/buffet")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(buffetId.toString()))
                .andExpect(jsonPath("$.nombre").value("Buffet del Colegio"))
                .andExpect(jsonPath("$.colegioId").value(colegioId.toString()));
    }

    @Test
    void obtenerBuffet_dadoAlumnoSinBuffet_deberiaRetornar404() throws Exception {
        UUID alumnoId = UUID.randomUUID();
        when(getBuffetByAlumno.execute(new GetBuffetByAlumnoRequest(alumnoId))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/alumnos/" + alumnoId + "/buffet")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
