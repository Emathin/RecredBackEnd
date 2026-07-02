package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.AddHealthRestriction;
import com.recred.backend.application.AddHealthRestrictionRequest;
import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.DetectStudentPreferencesRequest;
import com.recred.backend.application.GetHealthRestrictions;
import com.recred.backend.application.GetHealthRestrictionsRequest;
import com.recred.backend.application.GetHealthRestrictionsResponse;
import com.recred.backend.application.ManageRestrictions;
import com.recred.backend.application.ManageRestrictionsRequest;
import com.recred.backend.application.ManualProductLock;
import com.recred.backend.application.ManualProductLockRequest;
import com.recred.backend.application.ManualProductUnlock;
import com.recred.backend.application.ManualProductUnlockRequest;
import com.recred.backend.application.RemoveHealthRestrictions;
import com.recred.backend.application.RemoveHealthRestrictionsRequest;
import com.recred.backend.config.CustomAuthenticationEntryPoint;
import com.recred.backend.config.CustomJwtAuthenticationConverter;
import com.recred.backend.infrastructure.web.ActualizarRestriccionesDTO;
import com.recred.backend.infrastructure.web.ControlParentalController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ControlParentalController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
class ControlParentalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ManualProductLock manualProductLock;

    @MockitoBean
    private ManualProductUnlock manualProductUnlock;

    @MockitoBean
    private ManageRestrictions manageRestrictions;

    @MockitoBean
    private GetHealthRestrictions getHealthRestrictions;

    @MockitoBean
    private AddHealthRestriction addHealthRestriction;

    @MockitoBean
    private RemoveHealthRestrictions removeHealthRestrictions;

    @MockitoBean
    private DetectStudentPreferences detectStudentPreferences;

    @MockitoBean
    private CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private UUID alumnoId;
    private UUID clasificacionId;

    @BeforeEach
    void setUp() {
        alumnoId = UUID.randomUUID();
        clasificacionId = UUID.randomUUID();
    }

    @Test
    void actualizarRestriccionesSalud_DeberiaRetornar200Ok() throws Exception {
        ActualizarRestriccionesDTO dto = new ActualizarRestriccionesDTO();
        dto.setClasificacionesIds(List.of(clasificacionId));

        mockMvc.perform(put("/api/v1/control-parental/alumnos/{alumnoId}/actualizar-restricciones-salud", alumnoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(manageRestrictions).execute(new ManageRestrictionsRequest(alumnoId, dto.getClasificacionesIds()));
    }

    @Test
    void obtenerRestriccionesSalud_DeberiaRetornarListaY200Ok() throws Exception {
        GetHealthRestrictionsResponse responseItem = new GetHealthRestrictionsResponse(
                clasificacionId, "Celíaco", true
        );

        when(getHealthRestrictions.ejecutar(new GetHealthRestrictionsRequest(alumnoId)))
                .thenReturn(Set.of(responseItem));

        mockMvc.perform(get("/api/v1/control-parental/alumnos/{alumnoId}/obtener-restricciones-salud", alumnoId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(clasificacionId.toString()))
                .andExpect(jsonPath("$[0].descripcion").value("Celíaco"));

        verify(getHealthRestrictions).ejecutar(new GetHealthRestrictionsRequest(alumnoId));
    }

    @Test
    void agregarRestriccionSalud_DeberiaRetornar200Ok() throws Exception {
        mockMvc.perform(post("/api/v1/control-parental/alumnos/{alumnoId}/restricciones-salud/{clasificacionId}", alumnoId, clasificacionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(addHealthRestriction).execute(new AddHealthRestrictionRequest(alumnoId, clasificacionId));
    }

    @Test
    void removerRestriccionSalud_DeberiaRetornar204NoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/control-parental/alumnos/{alumnoId}/restricciones-salud/{clasificacionId}", alumnoId, clasificacionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(removeHealthRestrictions).execute(
                new RemoveHealthRestrictionsRequest(alumnoId, clasificacionId));
    }
    @Test
    void returnAcceptedWhenPreferenceAnalysisIsTriggered() throws Exception {
        UUID buffetId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/control-parental/alumnos/{alumnoId}/analizar-preferencias", alumnoId)
                        .param("buffetId", buffetId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        verify(detectStudentPreferences).execute(
                new DetectStudentPreferencesRequest(alumnoId, buffetId)
        );
    }

    @Test
    void returnInternalServerErrorWhenPreferenceAnalysisFails() throws Exception {
        UUID buffetId = UUID.randomUUID();
        doThrow(new RuntimeException("Error IA")).when(detectStudentPreferences)
                .execute(new DetectStudentPreferencesRequest(alumnoId, buffetId));

        mockMvc.perform(post("/api/v1/control-parental/alumnos/{alumnoId}/analizar-preferencias", alumnoId)
                        .param("buffetId", buffetId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void cuandoSeHacePostParaBloquear_entoncesRetorna200Ok() throws Exception {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();

        when(manualProductLock.execute(new ManualProductLockRequest(alumnoId, productoId)))
                .thenReturn(new com.recred.backend.application.ManualProductLockResponse());

        mockMvc.perform(post("/api/v1/control-parental/alumnos/{alumnoId}/productos-bloqueados/{productoId}", alumnoId, productoId))
                .andExpect(status().isOk());
    }

    @Test
    void cuandoSeHaceDeleteParaDesbloquear_entoncesRetorna200Ok() throws Exception {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();

        when(manualProductUnlock.execute(new ManualProductUnlockRequest(alumnoId, productoId)))
                .thenReturn(new com.recred.backend.application.ManualProductUnlockResponse());

        mockMvc.perform(delete("/api/v1/control-parental/alumnos/{alumnoId}/productos-bloqueados/{productoId}", alumnoId, productoId))
                .andExpect(status().isOk());

        verify(manualProductUnlock).execute(new ManualProductUnlockRequest(alumnoId, productoId));
    }
}
