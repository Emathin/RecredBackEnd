package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.infrastructure.web.*;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlumnoControllerTest {

    private AlumnoRepository alumnoRepository;
    private UsuarioService usuarioService;
    private AlumnoController controller;

    private final UUID ALUMNO_ID = UUID.randomUUID();
    private final String NOMBRE = "Ana";
    private final String APELLIDO = "Lopez";
    private final String DNI = "12345678";
    private final String STUDENT_SUB = "student-sub";
    private final int HTTP_OK = 200;
    private final int HTTP_NOT_FOUND = 404;

    @BeforeEach
    void setUp() {
        alumnoRepository = mock(AlumnoRepository.class);
        usuarioService = mock(UsuarioService.class);
        controller = new AlumnoController(alumnoRepository, usuarioService);
    }

    @Test
    void getAllAlumnosReturnsMappedStudents() {
        givenRepositoryFindsStudents();
        
        ResponseEntity<List<AlumnoController.AlumnoDTO>> response = whenGetAllAlumnos();
        
        thenResponseHasMappedStudents(response);
    }

    @Test
    void getAllAlumnosMapsNullUserDniAsNull() {
        givenRepositoryFindsStudentWithNullDni();
        
        ResponseEntity<List<AlumnoController.AlumnoDTO>> response = whenGetAllAlumnos();
        
        thenResponseHasStudentWithNullDni(response);
    }

    @Test
    void getMiPerfilReturnsStudentWhenUserHasAlumnoAndRepositoryFindsIt() {
        givenUserHasAlumnoProfile();
        givenRepositoryFindsAlumno();
        
        ResponseEntity<StudentDTO> response = whenGetMiPerfil();
        
        thenResponseHasStudentProfile(response);
    }

    @Test
    void getMiPerfilReturnsNotFoundWhenUserHasNoAlumno() {
        givenUserWithoutAlumnoProfile();
        
        ResponseEntity<StudentDTO> response = whenGetMiPerfil();
        
        thenResponseIsNotFound(response);
    }

    @Test
    void getMiPerfilReturnsNotFoundWhenRepositoryDoesNotFindAlumno() {
        givenUserHasAlumnoProfile();
        givenRepositoryDoesNotFindAlumno();
        
        ResponseEntity<StudentDTO> response = whenGetMiPerfil();
        
        thenResponseIsNotFound(response);
    }

    private void givenRepositoryFindsStudents() {
        Alumno alumno = createAlumno(ALUMNO_ID, NOMBRE, APELLIDO, DNI);
        when(alumnoRepository.findAll()).thenReturn(List.of(alumno));
    }

    private void givenRepositoryFindsStudentWithNullDni() {
        Alumno alumno = new Alumno();
        alumno.setId(ALUMNO_ID);
        alumno.setNombre(NOMBRE);
        alumno.setApellido(APELLIDO);
        when(alumnoRepository.findAll()).thenReturn(List.of(alumno));
    }

    private void givenUserHasAlumnoProfile() {
        Usuario usuario = new Usuario();
        Alumno perfil = new Alumno();
        perfil.setId(ALUMNO_ID);
        usuario.setAlumno(perfil);
        when(usuarioService.obtenerUsuarioActual(STUDENT_SUB)).thenReturn(usuario);
    }

    private void givenUserWithoutAlumnoProfile() {
        when(usuarioService.obtenerUsuarioActual(STUDENT_SUB)).thenReturn(new Usuario());
    }

    private void givenRepositoryFindsAlumno() {
        Alumno alumno = createAlumno(ALUMNO_ID, NOMBRE, APELLIDO, DNI);
        when(alumnoRepository.findById(ALUMNO_ID)).thenReturn(Optional.of(alumno));
    }

    private void givenRepositoryDoesNotFindAlumno() {
        when(alumnoRepository.findById(ALUMNO_ID)).thenReturn(Optional.empty());
    }

    private ResponseEntity<List<AlumnoController.AlumnoDTO>> whenGetAllAlumnos() {
        return controller.getAllAlumnos();
    }

    private ResponseEntity<StudentDTO> whenGetMiPerfil() {
        return controller.getMiPerfil(new TestingAuthenticationToken(STUDENT_SUB, ""));
    }

    private void thenResponseHasMappedStudents(
            ResponseEntity<List<AlumnoController.AlumnoDTO>> response) {
        assertEquals(HTTP_OK, response.getStatusCode().value());
        assertEquals(ALUMNO_ID, response.getBody().get(0).id());
        assertEquals(NOMBRE, response.getBody().get(0).nombre());
        assertEquals(DNI, response.getBody().get(0).dni());
    }

    private void thenResponseHasStudentWithNullDni(
            ResponseEntity<List<AlumnoController.AlumnoDTO>> response) {
        assertEquals(ALUMNO_ID, response.getBody().get(0).id());
        assertNull(response.getBody().get(0).dni());
    }

    private void thenResponseHasStudentProfile(ResponseEntity<StudentDTO> response) {
        assertEquals(HTTP_OK, response.getStatusCode().value());
        assertEquals(ALUMNO_ID, response.getBody().id());
        assertEquals(NOMBRE, response.getBody().nombre());
    }

    private void thenResponseIsNotFound(ResponseEntity<StudentDTO> response) {
        assertEquals(HTTP_NOT_FOUND, response.getStatusCode().value());
    }

    private Alumno createAlumno(UUID alumnoId, String nombre, String apellido, String dni) {
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
