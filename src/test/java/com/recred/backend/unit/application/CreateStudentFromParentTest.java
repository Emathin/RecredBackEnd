package com.recred.backend.unit.application;

import com.recred.backend.application.CreateStudentFromParent;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.GradoRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.CognitoStudentIdentityService;
import com.recred.backend.domain.services.CreatedCognitoUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateStudentFromParentTest {

    private static final String PARENT_SUB = "parent-sub";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private GradoRepository gradoRepository;
    @Mock
    private CognitoStudentIdentityService cognitoStudentIdentityService;

    @Captor
    private ArgumentCaptor<Usuario> usuarioCaptor;
    @Captor
    private ArgumentCaptor<Alumno> alumnoCaptor;
    @Captor
    private ArgumentCaptor<Tutor> tutorCaptor;

    private CreateStudentFromParent createStudentFromParent;

    @BeforeEach
    void setUp() {
        createStudentFromParent = new CreateStudentFromParent(
                usuarioRepository,
                tutorRepository,
                alumnoRepository,
                gradoRepository,
                cognitoStudentIdentityService);
    }

    @Test
    void parentCreatesStudentUserAndLinksItToTutor() {
        UUID gradoId = UUID.randomUUID();
        UUID savedUserId = UUID.randomUUID();
        UUID savedAlumnoId = UUID.randomUUID();
        Grado grado = new Grado();
        grado.setId(gradoId);

        givenParentWithTutor();
        when(usuarioRepository.findByEmail("juan.perez@example.com"))
                .thenReturn(Optional.empty());
        when(gradoRepository.findById(gradoId)).thenReturn(Optional.of(grado));
        givenCognitoCreatesStudent("juan.perez", "juan.perez@example.com");
        persistSavedEntities(savedUserId, savedAlumnoId);

        Alumno result = executeValidRequest(gradoId);

        verify(usuarioRepository).save(usuarioCaptor.capture());
        verify(alumnoRepository).save(alumnoCaptor.capture());
        verify(tutorRepository).save(tutorCaptor.capture());

        assertCreatedUser(usuarioCaptor.getValue());
        assertCreatedStudent(alumnoCaptor.getValue(), usuarioCaptor.getValue(), savedUserId,
                grado);
        assertTrue(tutorCaptor.getValue().getHijos().contains(result));
        assertTrue(result.getTutores().contains(tutorCaptor.getValue()));
        assertEquals(savedAlumnoId, result.getId());
    }

    @Test
    void nonParentUserCannotCreateStudent() {
        Usuario parent = user(UUID.randomUUID(), RolUsuario.ALUMNO);
        when(usuarioRepository.findByIdCognito(PARENT_SUB)).thenReturn(Optional.of(parent));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                createStudentFromParent.execute(PARENT_SUB, "juan", "Juan",
                        "Perez", "juan@example.com", null, null));

        assertEquals("El usuario autenticado no tiene rol PADRE", exception.getMessage());
        verify(cognitoStudentIdentityService, never())
                .createStudentUser(any(), any(), any(), any());
        verify(usuarioRepository, never()).save(any());
        verify(alumnoRepository, never()).save(any());
    }

    @Test
    void duplicatedEmailStopsBeforeCreatingCognitoUser() {
        UUID parentUserId = UUID.randomUUID();
        Usuario parent = user(parentUserId, RolUsuario.PADRE);
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());

        when(usuarioRepository.findByIdCognito(PARENT_SUB)).thenReturn(Optional.of(parent));
        when(tutorRepository.findByUsuarioId(parentUserId)).thenReturn(Optional.of(tutor));
        when(usuarioRepository.findByEmail("juan@example.com"))
                .thenReturn(Optional.of(user(UUID.randomUUID(), RolUsuario.ALUMNO)));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                createStudentFromParent.execute(PARENT_SUB, "juan", "Juan",
                        "Perez", " juan@example.com ", null, null));

        assertEquals("Ya existe un usuario con el email indicado", exception.getMessage());
        verify(cognitoStudentIdentityService, never())
                .createStudentUser(any(), any(), any(), any());
        verify(usuarioRepository, never()).save(any());
        verify(alumnoRepository, never()).save(any());
    }

    @Test
    void missingGradeStopsBeforeCreatingCognitoUser() {
        UUID parentUserId = UUID.randomUUID();
        UUID gradoId = UUID.randomUUID();
        Usuario parent = user(parentUserId, RolUsuario.PADRE);
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());

        when(usuarioRepository.findByIdCognito(PARENT_SUB)).thenReturn(Optional.of(parent));
        when(tutorRepository.findByUsuarioId(parentUserId)).thenReturn(Optional.of(tutor));
        when(usuarioRepository.findByEmail("juan@example.com"))
                .thenReturn(Optional.empty());
        when(gradoRepository.findById(gradoId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                createStudentFromParent.execute(PARENT_SUB, "juan", "Juan",
                        "Perez", "juan@example.com", null, gradoId));

        assertEquals("No se encontro el grado indicado", exception.getMessage());
        verify(cognitoStudentIdentityService, never())
                .createStudentUser(any(), any(), any(), any());
        verify(usuarioRepository, never()).save(any());
        verify(alumnoRepository, never()).save(any());
    }

    @Test
    void emailLikeUsernameIsRejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                createStudentFromParent.execute(PARENT_SUB, "juan@example.com", "Juan",
                        "Perez", "juan@example.com", null, null));

        assertEquals("El username no puede tener formato de email", exception.getMessage());
        verify(usuarioRepository, never()).findByIdCognito(any());
        verify(cognitoStudentIdentityService, never())
                .createStudentUser(any(), any(), any(), any());
    }

    @Test
    void cognitoFailureStopsLocalPersistence() {
        UUID parentUserId = UUID.randomUUID();
        Usuario parent = user(parentUserId, RolUsuario.PADRE);
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());

        when(usuarioRepository.findByIdCognito(PARENT_SUB)).thenReturn(Optional.of(parent));
        when(tutorRepository.findByUsuarioId(parentUserId)).thenReturn(Optional.of(tutor));
        when(usuarioRepository.findByEmail("juan@example.com"))
                .thenReturn(Optional.empty());
        when(cognitoStudentIdentityService.createStudentUser(
                "juan", "juan@example.com", "Juan", "Perez"))
                .thenThrow(new IllegalStateException("No se pudo crear el usuario en Cognito"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                createStudentFromParent.execute(PARENT_SUB, "juan", "Juan",
                        "Perez", "juan@example.com", null, null));

        assertEquals("No se pudo crear el usuario en Cognito", exception.getMessage());
        verify(usuarioRepository, never()).save(any());
        verify(alumnoRepository, never()).save(any());
        verify(tutorRepository, never()).save(any());
    }

    private Usuario user(UUID id, RolUsuario rol) {
        Usuario user = new Usuario();
        user.setId(id);
        user.setRol(rol);
        return user;
    }

    private Tutor givenParentWithTutor() {
        UUID parentUserId = UUID.randomUUID();
        Usuario parent = user(parentUserId, RolUsuario.PADRE);
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());

        when(usuarioRepository.findByIdCognito(PARENT_SUB)).thenReturn(Optional.of(parent));
        when(tutorRepository.findByUsuarioId(parentUserId)).thenReturn(Optional.of(tutor));
        return tutor;
    }

    private void givenCognitoCreatesStudent(String username, String email) {
        when(cognitoStudentIdentityService.createStudentUser(username, email, "Juan", "Perez"))
                .thenReturn(new CreatedCognitoUser("student-sub", username));
    }

    private void persistSavedEntities(UUID savedUserId, UUID savedAlumnoId) {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario saved = invocation.getArgument(0);
            saved.setId(savedUserId);
            return saved;
        });
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(invocation -> {
            Alumno saved = invocation.getArgument(0);
            saved.setId(savedAlumnoId);
            return saved;
        });
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(invocation ->
                invocation.getArgument(0));
    }

    private Alumno executeValidRequest(UUID gradoId) {
        return createStudentFromParent.execute(
                PARENT_SUB,
                " juan.perez ",
                " Juan ",
                " Perez ",
                " juan.perez@example.com ",
                " 12345678 ",
                gradoId);
    }

    private void assertCreatedUser(Usuario savedUser) {
        assertEquals("student-sub", savedUser.getIdCognito());
        assertEquals("juan.perez@example.com", savedUser.getEmail());
        assertEquals("Juan", savedUser.getNombre());
        assertEquals("Perez", savedUser.getApellido());
        assertEquals("12345678", savedUser.getDni());
        assertEquals(RolUsuario.ALUMNO, savedUser.getRol());
        assertEquals(Boolean.TRUE, savedUser.getAutenticadoConCognito());
        assertEquals(Boolean.TRUE, savedUser.getActivo());
        assertNotNull(savedUser.getCreadoEn());
        assertNotNull(savedUser.getActualizadoEn());
    }

    private void assertCreatedStudent(Alumno savedAlumno, Usuario savedUser, UUID savedUserId,
                                      Grado grado) {
        assertSame(savedUser, savedAlumno.getUsuario());
        assertEquals(savedUserId, savedAlumno.getUsuarioId());
        assertEquals(Boolean.TRUE, savedAlumno.getActivo());
        assertSame(grado, savedAlumno.getGrado());
    }
}
