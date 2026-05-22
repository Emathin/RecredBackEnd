package com.recred.backend.control_parental.infrastructure.web;

import com.recred.backend.control_parental.application.usecases.ActualizarRestriccionesSaludUseCase;
import tools.jackson.databind.ObjectMapper;
import com.recred.backend.control_parental.infrastructure.web.dtos.ActualizarRestriccionesDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ControlParentalController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ControlParentalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ActualizarRestriccionesSaludUseCase useCase;

    @Test
    void actualizarRestriccionesSalud_DebeRetornar200Ok_CuandoElRequestEsValido() throws Exception {
        UUID alumnoId = UUID.randomUUID();
        ActualizarRestriccionesDTO dto = new ActualizarRestriccionesDTO();
        dto.setClasificacionesIds(List.of(UUID.randomUUID()));

        doNothing().when(useCase).ejecutar(eq(alumnoId), anyList());

        mockMvc.perform(put("/api/control-parental/alumnos/{alumnoId}/restricciones-salud", alumnoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(useCase).ejecutar(eq(alumnoId), anyList());
    }
}