package com.recred.backend.infrastructure.web;

import com.recred.backend.application.AdministrarRestricciones;
import com.recred.backend.application.AgregarRestriccionSalud;
import com.recred.backend.application.ObtenerRestriccionesSalud;
import com.recred.backend.application.RemoverRestriccionSalud;
import com.recred.backend.domain.models.ClasificacionSalud;


import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import java.util.List;
import java.util.Set;
import java.util.UUID;

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
    private AdministrarRestricciones administrarRestricciones;

    @MockitoBean
    private ObtenerRestriccionesSalud obtenerRestriccionesSalud;

    @MockitoBean
    private AgregarRestriccionSalud agregarRestriccionSalud;

    @MockitoBean
    private RemoverRestriccionSalud removerRestriccionSalud;

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

        mockMvc.perform(put("/api/control-parental/alumnos/{alumnoId}/actualizar-restricciones-salud", alumnoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(administrarRestricciones).ejecutar(alumnoId, dto.getClasificacionesIds());
    }

    @Test
    void obtenerRestriccionesSalud_DeberiaRetornarListaY200Ok() throws Exception {
        ClasificacionSalud clasificacion = new ClasificacionSalud();
        clasificacion.setId(clasificacionId);
        clasificacion.setDescripcion("Celíaco");

        when(obtenerRestriccionesSalud.ejecutar(alumnoId)).thenReturn(Set.of(clasificacion));

        mockMvc.perform(get("/api/control-parental/alumnos/{alumnoId}/obtener-restricciones-salud", alumnoId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(clasificacionId.toString()))
                .andExpect(jsonPath("$[0].descripcion").value("Celíaco"));

        verify(obtenerRestriccionesSalud).ejecutar(alumnoId);
    }

    @Test
    void agregarRestriccionSalud_DeberiaRetornar200Ok() throws Exception {
        mockMvc.perform(post("/api/control-parental/alumnos/{alumnoId}/restricciones-salud/{clasificacionId}", alumnoId, clasificacionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(agregarRestriccionSalud).ejecutar(alumnoId, clasificacionId);
    }

    @Test
    void removerRestriccionSalud_DeberiaRetornar204NoContent() throws Exception {
        mockMvc.perform(delete("/api/control-parental/alumnos/{alumnoId}/restricciones-salud/{clasificacionId}", alumnoId, clasificacionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(removerRestriccionSalud).ejecutar(alumnoId, clasificacionId);
    }
}