package com.recred.backend.web;

import com.recred.backend.application.AddHealthRestriction;
import com.recred.backend.application.AddHealthRestrictionRequest;
import com.recred.backend.application.BloquearProductoManual;
import com.recred.backend.application.BloquearProductoManualRequest;
import com.recred.backend.application.DesbloquearProductoManual;
import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.GetHealthRestrictions;
import com.recred.backend.application.ManageRestrictions;
import com.recred.backend.application.RemoveHealthRestrictions;
import com.recred.backend.config.CustomAuthenticationEntryPoint;
import com.recred.backend.config.CustomJwtAuthenticationConverter;
import com.recred.backend.domain.models.ClasificacionSalud;
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

import static org.mockito.Mockito.doNothing;
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
    private BloquearProductoManual bloquearProductoManual;

    @MockitoBean
    private DesbloquearProductoManual desbloquearProductoManual;

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

        verify(manageRestrictions).execute(alumnoId, dto.getClasificacionesIds());
    }

    @Test
    void obtenerRestriccionesSalud_DeberiaRetornarListaY200Ok() throws Exception {
        ClasificacionSalud clasificacion = new ClasificacionSalud();
        clasificacion.setId(clasificacionId);
        clasificacion.setDescripcion("Celíaco");

        when(getHealthRestrictions.ejecutar(alumnoId)).thenReturn(Set.of(clasificacion));

        mockMvc.perform(get("/api/v1/control-parental/alumnos/{alumnoId}/obtener-restricciones-salud", alumnoId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(clasificacionId.toString()))
                .andExpect(jsonPath("$[0].descripcion").value("Celíaco"));

        verify(getHealthRestrictions).ejecutar(alumnoId);
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

        verify(removeHealthRestrictions).ejecutar(alumnoId, clasificacionId);
    }
    @Test
    void returnAcceptedWhenPreferenceAnalysisIsTriggered() throws Exception {
        UUID buffetId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/control-parental/alumnos/{alumnoId}/analizar-preferencias", alumnoId)
                        .param("buffetId", buffetId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        verify(detectStudentPreferences).execute(alumnoId, buffetId);
    }

    @Test
    void returnInternalServerErrorWhenPreferenceAnalysisFails() throws Exception {
        UUID buffetId = UUID.randomUUID();
        doThrow(new RuntimeException("Error IA")).when(detectStudentPreferences).execute(alumnoId, buffetId);

        mockMvc.perform(post("/api/v1/control-parental/alumnos/{alumnoId}/analizar-preferencias", alumnoId)
                        .param("buffetId", buffetId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void cuandoSeHacePostParaBloquear_entoncesRetorna200Ok() throws Exception {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();

        doNothing().when(bloquearProductoManual).execute(new BloquearProductoManualRequest(alumnoId, productoId));

        mockMvc.perform(post("/api/v1/control-parental/alumnos/{alumnoId}/productos-bloqueados/{productoId}", alumnoId, productoId))
                .andExpect(status().isOk());
    }

    @Test
    void cuandoSeHaceDeleteParaDesbloquear_entoncesRetorna200Ok() throws Exception {
        UUID alumnoId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();

        doNothing().when(desbloquearProductoManual).execute(alumnoId, productoId);

        mockMvc.perform(delete("/api/v1/control-parental/alumnos/{alumnoId}/productos-bloqueados/{productoId}", alumnoId, productoId))
                .andExpect(status().isOk());
    }
}
