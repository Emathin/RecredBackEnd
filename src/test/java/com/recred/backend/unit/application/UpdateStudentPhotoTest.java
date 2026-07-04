package com.recred.backend.unit.application;

import com.recred.backend.application.UpdateStudentPhoto;
import com.recred.backend.application.UpdateStudentPhotoRequest;
import com.recred.backend.application.UpdateStudentPhotoResponse;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class UpdateStudentPhotoTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private UpdateStudentPhoto updateStudentPhoto;

    private String parentCognitoSubject;
    private UUID parentUserId;
    private UUID tutorId;
    private UUID alumnoId;
    private String newPhotoUrl;
    private String studentCognitoId;

    private Usuario parentUser;
    private Tutor tutor;
    private Alumno alumno;
    private Usuario studentUser;

    @BeforeEach
    void setUp() {
        parentCognitoSubject = "parent-cognito-123";
        parentUserId = UUID.randomUUID();
        tutorId = UUID.randomUUID();
        alumnoId = UUID.randomUUID();
        newPhotoUrl = "https://example.com/new-photo.jpg";
        studentCognitoId = "student-cognito-456";

        parentUser = new Usuario();
        parentUser.setId(parentUserId);
        parentUser.setRol(RolUsuario.PADRE);

        tutor = new Tutor();
        tutor.setId(tutorId);
        tutor.setUsuario(parentUser);

        studentUser = new Usuario();
        studentUser.setId(UUID.randomUUID());
        studentUser.setIdCognito(studentCognitoId);
        studentUser.setNombre("Lucas");
        studentUser.setApellido("Perez");

        alumno = new Alumno();
        alumno.setId(alumnoId);
        alumno.setUsuario(studentUser);
    }

    @Test
    void execute_CuandoTodoEsValido_ActualizaFotoYRetornaResponse() {
        tutor.addHijo(alumno);
        UpdateStudentPhotoRequest request = request();

        when(usuarioRepository.findByIdCognito(parentCognitoSubject)).thenReturn(Optional.of(parentUser));
        when(tutorRepository.findByUsuarioId(parentUserId)).thenReturn(Optional.of(tutor));
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        UpdateStudentPhotoResponse response = updateStudentPhoto.execute(request);

        assertNotNull(response);
        assertEquals(alumnoId, response.id());
        assertEquals("Lucas", response.nombre());
        assertEquals("Perez", response.apellido());
        assertEquals(newPhotoUrl, response.urlFotoPerfil());
        assertEquals(newPhotoUrl, alumno.getUsuario().getUrlFotoPerfil());

        verify(usuarioRepository).findByIdCognito(parentCognitoSubject);
        verify(tutorRepository).findByUsuarioId(parentUserId);
        verify(alumnoRepository).findById(alumnoId);
        verify(usuarioRepository).updateFotoPerfil(studentCognitoId, newPhotoUrl);
    }

    @Test
    void execute_CuandoUsuarioNoExiste_LanzaIllegalArgumentException() {
        UpdateStudentPhotoRequest request = request();

        when(usuarioRepository.findByIdCognito(parentCognitoSubject)).thenReturn(Optional.empty());

        Executable execution = () -> updateStudentPhoto.execute(request);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);
        assertEquals("Usuario autenticado no encontrado", exception.getMessage());

        verify(usuarioRepository).findByIdCognito(parentCognitoSubject);
        verify(tutorRepository, never()).findByUsuarioId(any());
        verify(alumnoRepository, never()).findById(any());
        verify(usuarioRepository, never()).updateFotoPerfil(anyString(), anyString());
    }

    @Test
    void execute_CuandoUsuarioNoTieneRolPadre_LanzaAccessDeniedException() {
        parentUser.setRol(RolUsuario.ALUMNO);
        UpdateStudentPhotoRequest request = request();

        when(usuarioRepository.findByIdCognito(parentCognitoSubject)).thenReturn(Optional.of(parentUser));

        Executable execution = () -> updateStudentPhoto.execute(request);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, execution);
        assertEquals("El usuario autenticado no tiene rol PADRE", exception.getMessage());

        verify(usuarioRepository).findByIdCognito(parentCognitoSubject);
        verify(tutorRepository, never()).findByUsuarioId(any());
        verify(alumnoRepository, never()).findById(any());
        verify(usuarioRepository, never()).updateFotoPerfil(anyString(), anyString());
    }

    @Test
    void execute_CuandoTutorNoExiste_LanzaIllegalArgumentException() {
        UpdateStudentPhotoRequest request = request();

        when(usuarioRepository.findByIdCognito(parentCognitoSubject)).thenReturn(Optional.of(parentUser));
        when(tutorRepository.findByUsuarioId(parentUserId)).thenReturn(Optional.empty());

        Executable execution = () -> updateStudentPhoto.execute(request);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);
        assertEquals("Tutor no encontrado para el usuario autenticado", exception.getMessage());

        verify(usuarioRepository).findByIdCognito(parentCognitoSubject);
        verify(tutorRepository).findByUsuarioId(parentUserId);
        verify(alumnoRepository, never()).findById(any());
        verify(usuarioRepository, never()).updateFotoPerfil(anyString(), anyString());
    }

    @Test
    void execute_CuandoAlumnoNoExiste_LanzaAlumnoNoEncontradoException() {
        UpdateStudentPhotoRequest request = request();

        when(usuarioRepository.findByIdCognito(parentCognitoSubject)).thenReturn(Optional.of(parentUser));
        when(tutorRepository.findByUsuarioId(parentUserId)).thenReturn(Optional.of(tutor));
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.empty());

        Executable execution = () -> updateStudentPhoto.execute(request);

        AlumnoNoEncontradoException exception = assertThrows(AlumnoNoEncontradoException.class, execution);
        assertEquals("No existe el alumno con id " + alumnoId, exception.getMessage());

        verify(usuarioRepository).findByIdCognito(parentCognitoSubject);
        verify(tutorRepository).findByUsuarioId(parentUserId);
        verify(alumnoRepository).findById(alumnoId);
        verify(usuarioRepository, never()).updateFotoPerfil(anyString(), anyString());
    }

    @Test
    void execute_CuandoAlumnoNoPerteneceAlTutor_LanzaAccessDeniedException() {
        UpdateStudentPhotoRequest request = request();

        when(usuarioRepository.findByIdCognito(parentCognitoSubject)).thenReturn(Optional.of(parentUser));
        when(tutorRepository.findByUsuarioId(parentUserId)).thenReturn(Optional.of(tutor));
        when(alumnoRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));

        Executable execution = () -> updateStudentPhoto.execute(request);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, execution);
        assertEquals("El alumno no pertenece al tutor autenticado", exception.getMessage());

        verify(usuarioRepository).findByIdCognito(parentCognitoSubject);
        verify(tutorRepository).findByUsuarioId(parentUserId);
        verify(alumnoRepository).findById(alumnoId);
        verify(usuarioRepository, never()).updateFotoPerfil(anyString(), anyString());
    }

    private UpdateStudentPhotoRequest request() {
        return new UpdateStudentPhotoRequest(parentCognitoSubject, alumnoId, newPhotoUrl);
    }
}
