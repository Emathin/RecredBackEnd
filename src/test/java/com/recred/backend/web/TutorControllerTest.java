package com.recred.backend.web;

import com.recred.backend.application.CreateStudentFromParent;
import com.recred.backend.application.CreateStudentFromParentRequest;
import com.recred.backend.application.CreateStudentFromParentResponse;
import com.recred.backend.application.GetChildrenByTutor;
import com.recred.backend.application.GetChildrenByTutorRequest;
import com.recred.backend.application.GetChildrenByTutorResponse;
import com.recred.backend.application.UpdateStudentPhoto;
import com.recred.backend.config.CustomAuthenticationEntryPoint;
import com.recred.backend.config.CustomJwtAuthenticationConverter;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.infrastructure.cloudinary.CloudinaryService;
import com.recred.backend.infrastructure.web.CreateChildRequest;
import com.recred.backend.infrastructure.web.StudentDTO;
import com.recred.backend.infrastructure.web.TutorController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private GetChildrenByTutor getChildrenByTutor;

    @MockitoBean
    private CreateStudentFromParent createStudentFromParent;

    @MockitoBean
    private CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    @MockitoBean
    private com.recred.backend.domain.services.UsuarioService usuarioService;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UpdateStudentPhoto updateStudentPhoto;

    @MockitoBean
    private CloudinaryService cloudinaryService;

    @Test
    void dadoUnTutorConHijos_cuandoConsultaEndpoint_entoncesRetorna200YListaDeEstudiantes() throws Exception {
        UUID tutorId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        UUID colegioId = UUID.randomUUID();

        Usuario usuarioHijo = new Usuario();
        usuarioHijo.setNombre("Lucas");
        usuarioHijo.setApellido("Pérez");

        Colegio colegio = new Colegio();
        colegio.setId(colegioId);

        Grado grado = new Grado();
        grado.setAnio("1");
        grado.setDivision("A");
        grado.setColegio(colegio);

        Billetera billetera = new Billetera();
        billetera.setSaldoCreditos(new BigDecimal("150.50"));

        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        alumno.setUsuario(usuarioHijo);
        alumno.setActivo(true);
        alumno.setGrado(grado);
        alumno.setBilletera(billetera);

        GetChildrenByTutorResponse childResponse = new GetChildrenByTutorResponse(
                alumnoId,
                "Lucas",
                "Pérez",
                true,
                "1 A",
                colegioId,
                new BigDecimal("150.50"),
                null
        );
        when(getChildrenByTutor.execute(GetChildrenByTutorRequest.byTutorId(tutorId)))
                .thenReturn(List.of(childResponse));

        mockMvc.perform(get("/api/v1/tutores/{tutorId}/hijos", tutorId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(alumnoId.toString()))
                .andExpect(jsonPath("$[0].nombre").value("Lucas"))
                .andExpect(jsonPath("$[0].apellido").value("Pérez"))
                .andExpect(jsonPath("$[0].activo").value(true))
                .andExpect(jsonPath("$[0].grado").value("1 A"))
                .andExpect(jsonPath("$[0].colegioId").value(colegioId.toString()))
                .andExpect(jsonPath("$[0].saldo").value(150.50));
    }

    @Test
    void dadoUnTutorAutenticado_cuandoConsultaMisHijos_entoncesUsaCognitoSubject()
            throws Exception {
        UUID alumnoId = UUID.randomUUID();
        GetChildrenByTutorResponse response = new GetChildrenByTutorResponse(
                alumnoId,
                "Ana",
                "Lopez",
                true,
                null,
                null,
                BigDecimal.ZERO,
                null
        );
        when(getChildrenByTutor.execute(GetChildrenByTutorRequest.byCognitoSubject("tutor-sub")))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/tutores/me/hijos")
                        .principal(new TestingAuthenticationToken("tutor-sub", ""))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(alumnoId.toString()))
                .andExpect(jsonPath("$[0].nombre").value("Ana"));

        verify(getChildrenByTutor).execute(GetChildrenByTutorRequest.byCognitoSubject("tutor-sub"));
    }

    @Test
    void dadoUnTutorAutenticado_cuandoCreaHijo_entoncesRetornaCreated() throws Exception {
        UUID alumnoId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();
        CreateStudentFromParentResponse alumno = new CreateStudentFromParentResponse(
                alumnoId,
                "Mateo",
                "Gomez",
                true,
                null,
                null,
                null,
                BigDecimal.ZERO,
                null
        );
        when(createStudentFromParent.execute(new CreateStudentFromParentRequest(
                "tutor-sub",
                "mateo",
                "Mateo",
                "Gomez",
                "mateo@test.com",
                "12345678",
                gradoId,
                null
        ))).thenReturn(alumno);

        mockMvc.perform(post("/api/v1/tutores/me/hijos")
                        .principal(new TestingAuthenticationToken("tutor-sub", ""))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "mateo",
                                  "nombre": "Mateo",
                                  "apellido": "Gomez",
                                  "email": "mateo@test.com",
                                  "dni": "12345678",
                                  "gradoId": "%s"
                                }
                                """.formatted(gradoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(alumnoId.toString()))
                .andExpect(jsonPath("$.nombre").value("Mateo"));

        verify(createStudentFromParent).execute(new CreateStudentFromParentRequest(
                "tutor-sub",
                "mateo",
                "Mateo",
                "Gomez",
                "mateo@test.com",
                "12345678",
                gradoId,
                null
        ));
    }

    @Test
    void obtenerHijosConTutorMeUsaTutorDelUsuarioActual() {
        UUID tutorId = UUID.randomUUID();
        UUID alumnoId = UUID.randomUUID();
        TutorController controller = new TutorController(
                getChildrenByTutor,
                usuarioService,
                createStudentFromParent,
                updateStudentPhoto,
                cloudinaryService
        );
        Usuario usuario = new Usuario();
        com.recred.backend.domain.models.Tutor tutor =
                new com.recred.backend.domain.models.Tutor();
        tutor.setId(tutorId);
        usuario.setTutor(tutor);

        GetChildrenByTutorResponse responseChild = new GetChildrenByTutorResponse(
                alumnoId,
                "Sofia",
                "Diaz",
                true,
                null,
                null,
                BigDecimal.ZERO,
                null
        );

        when(usuarioService.obtenerUsuarioActual("tutor-sub")).thenReturn(usuario);
        when(getChildrenByTutor.execute(GetChildrenByTutorRequest.byTutorId(tutorId)))
                .thenReturn(List.of(responseChild));

        ResponseEntity<List<StudentDTO>> response = controller.obtenerHijos(
                "me",
                new TestingAuthenticationToken("tutor-sub", "")
        );

        assertEquals(200, response.getStatusCode().value());
        assertEquals(alumnoId, response.getBody().get(0).id());
        verify(usuarioService).obtenerUsuarioActual("tutor-sub");
        verify(getChildrenByTutor).execute(GetChildrenByTutorRequest.byTutorId(tutorId));
    }

    @Test
    void obtenerHijosConTutorMeSinPerfilRetornaListaVacia() {
        TutorController controller = new TutorController(
                getChildrenByTutor,
                usuarioService,
                createStudentFromParent,
                updateStudentPhoto,
                cloudinaryService
        );
        Usuario usuario = new Usuario();
        when(usuarioService.obtenerUsuarioActual("tutor-sub")).thenReturn(usuario);

        ResponseEntity<List<StudentDTO>> response = controller.obtenerHijos(
                "me",
                new TestingAuthenticationToken("tutor-sub", "")
        );

        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of(), response.getBody());
    }

    @Test
    void crearHijoSinAutenticacionLanzaCredentialsException() {
        TutorController controller = new TutorController(
                getChildrenByTutor,
                usuarioService,
                createStudentFromParent,
                updateStudentPhoto,
                cloudinaryService
        );
        CreateChildRequest request = new CreateChildRequest(
                "mateo",
                "Mateo",
                "Gomez",
                "mateo@test.com",
                "12345678",
                UUID.randomUUID(),
                null
        );

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.crearHijo(null, request));
    }

    private Alumno alumno(UUID alumnoId, String nombre, String apellido) {
        Usuario usuarioHijo = new Usuario();
        usuarioHijo.setNombre(nombre);
        usuarioHijo.setApellido(apellido);

        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        alumno.setUsuario(usuarioHijo);
        alumno.setActivo(true);
        return alumno;
    }
}
