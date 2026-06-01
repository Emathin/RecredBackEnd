package com.recred.backend.infrastructure.web;

import com.recred.backend.application.ObtenerHijosPorPadre;
import com.recred.backend.config.CustomAuthenticationEntryPoint;
import com.recred.backend.config.CustomJwtAuthenticationConverter;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TutorController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class}
)
class TutorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ObtenerHijosPorPadre obtenerHijosPorPadre;

    @MockitoBean
    private CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void dadoUnTutorConHijos_cuandoConsultaEndpoint_entoncesRetorna200YListaDeEstudiantes() throws Exception {
        UUID tutorId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();

        Usuario usuarioHijo = new Usuario();
        usuarioHijo.setNombre("Lucas");
        usuarioHijo.setApellido("Pérez");

        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        alumno.setUsuario(usuarioHijo);
        alumno.setActivo(true);

        when(obtenerHijosPorPadre.execute(tutorId)).thenReturn(Set.of(alumno));

        mockMvc.perform(get("/api/v1/tutores/{tutorId}/hijos", tutorId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(alumnoId.toString()))
                .andExpect(jsonPath("$[0].nombre").value("Lucas"))
                .andExpect(jsonPath("$[0].apellido").value("Pérez"))
                .andExpect(jsonPath("$[0].activo").value(true));
    }
}