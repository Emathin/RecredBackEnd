package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.services.UsuarioService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlumnoControllerTest {

    private AlumnoRepository alumnoRepository;
    private UsuarioService usuarioService;
    private AlumnoController controller;

    @BeforeEach
    void setUp() {
        alumnoRepository = mock(AlumnoRepository.class);
        usuarioService = mock(UsuarioService.class);
        controller = new AlumnoController(alumnoRepository, usuarioService);
    }

    @Test
    void getAllAlumnosReturnsMappedStudents() {
        UUID alumnoId = UUID.randomUUID();
        Alumno alumno = alumno(alumnoId, "Ana", "Lopez", "12345678");
        when(alumnoRepository.findAll()).thenReturn(List.of(alumno));

        ResponseEntity<List<AlumnoController.AlumnoDTO>> response =
                controller.getAllAlumnos();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(alumnoId, response.getBody().get(0).id());
        assertEquals("Ana", response.getBody().get(0).nombre());
        assertEquals("12345678", response.getBody().get(0).dni());
    }

    @Test
    void getAllAlumnosMapsNullUserDniAsNull() {
        UUID alumnoId = UUID.randomUUID();
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        alumno.setNombre("Ana");
        alumno.setApellido("Lopez");
        when(alumnoRepository.findAll()).thenReturn(List.of(alumno));

        ResponseEntity<List<AlumnoController.AlumnoDTO>> response =
                controller.getAllAlumnos();

        assertEquals(alumnoId, response.getBody().get(0).id());
        assertEquals(null, response.getBody().get(0).dni());
    }

    @Test
    void getMiPerfilReturnsStudentWhenUserHasAlumnoAndRepositoryFindsIt() {
        UUID alumnoId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        Alumno perfil = new Alumno();
        perfil.setId(alumnoId);
        usuario.setAlumno(perfil);
        Alumno alumno = alumno(alumnoId, "Ana", "Lopez", "12345678");
        when(usuarioService.obtenerUsuarioActual("student-sub")).thenReturn(usuario);
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        ResponseEntity<StudentDTO> response = controller.getMiPerfil(
                new TestingAuthenticationToken("student-sub", "")
        );

        assertEquals(200, response.getStatusCode().value());
        assertEquals(alumnoId, response.getBody().id());
        assertEquals("Ana", response.getBody().nombre());
    }

    @Test
    void getMiPerfilReturnsNotFoundWhenUserHasNoAlumno() {
        when(usuarioService.obtenerUsuarioActual("student-sub")).thenReturn(new Usuario());

        ResponseEntity<StudentDTO> response = controller.getMiPerfil(
                new TestingAuthenticationToken("student-sub", "")
        );

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void getMiPerfilReturnsNotFoundWhenRepositoryDoesNotFindAlumno() {
        UUID alumnoId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        Alumno perfil = new Alumno();
        perfil.setId(alumnoId);
        usuario.setAlumno(perfil);
        when(usuarioService.obtenerUsuarioActual("student-sub")).thenReturn(usuario);
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        ResponseEntity<StudentDTO> response = controller.getMiPerfil(
                new TestingAuthenticationToken("student-sub", "")
        );

        assertEquals(404, response.getStatusCode().value());
    }

    private Alumno alumno(UUID alumnoId, String nombre, String apellido, String dni) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setDni(dni);

        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        alumno.setNombre(nombre);
        alumno.setApellido(apellido);
        alumno.setUsuario(usuario);
        alumno.setActivo(true);
        return alumno;
    }
}
