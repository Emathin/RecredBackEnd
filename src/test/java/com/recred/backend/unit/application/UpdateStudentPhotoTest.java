package com.recred.backend.unit.application;

import com.recred.backend.application.UpdateStudentPhoto;
import com.recred.backend.application.UpdateStudentPhotoRequest;
import com.recred.backend.application.UpdateStudentPhotoResponse;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.OperationNotAllowedException;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateStudentPhotoTest {

    private static final String PARENT_COGNITO = "parent-cognito-123";
    private static final UUID PARENT_ID = UUID.randomUUID();
    private static final UUID TUTOR_ID = UUID.randomUUID();
    private static final UUID ALUMNO_ID = UUID.randomUUID();
    private static final String NEW_PHOTO_URL = "https://example.com/new-photo.jpg";
    private static final String STUDENT_COGNITO = "student-cognito-456";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private AlumnoRepository alumnoRepository;

    private UpdateStudentPhoto updateStudentPhoto;

    @BeforeEach
    void setUp() {
        updateStudentPhoto = new UpdateStudentPhoto(
                usuarioRepository, tutorRepository, alumnoRepository);
    }

    @Test
    void shouldUpdatePhotoAndReturnResponseWhenEverythingIsValid() {
        Alumno alumno = givenParentTutorAndStudentExist(true);

        UpdateStudentPhotoResponse response = whenUpdatingPhoto();

        thenPhotoIsUpdatedInRepository();
        thenResponseContainsUpdatedPhoto(response, alumno);
    }

    @Test
    void shouldThrowExceptionWhenParentUserDoesNotExist() {
        givenParentUserNotFound();

        Executable execution = this::whenUpdatingPhoto;

        thenThrowsIllegalArgumentExceptionWithMessage(execution, "Usuario autenticado no encontrado");
        thenNoFurtherActionIsTaken();
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotHaveParentRole() {
        givenUserExistsWithRole(RolUsuario.ALUMNO);

        Executable execution = this::whenUpdatingPhoto;

        thenThrowsOperationNotAllowedExceptionWithMessage(execution, "El usuario autenticado no tiene rol PADRE");
        thenNoFurtherActionIsTaken();
    }

    @Test
    void shouldThrowExceptionWhenTutorDoesNotExist() {
        givenUserExistsWithRole(RolUsuario.PADRE);
        givenTutorNotFound();

        Executable execution = this::whenUpdatingPhoto;

        thenThrowsIllegalArgumentExceptionWithMessage(execution, "Tutor no encontrado para el usuario autenticado");
        thenNoStudentActionIsTaken();
    }

    @Test
    void shouldThrowExceptionWhenStudentDoesNotExist() {
        givenParentTutorAndStudentNotFound();

        Executable execution = this::whenUpdatingPhoto;

        thenThrowsAlumnoNoEncontradoException(execution, ALUMNO_ID);
        thenPhotoIsNotUpdated();
    }

    @Test
    void shouldThrowExceptionWhenStudentDoesNotBelongToTutor() {
        givenParentTutorAndStudentExist(false);

        Executable execution = this::whenUpdatingPhoto;

        thenThrowsOperationNotAllowedExceptionWithMessage(execution, "El alumno no pertenece al tutor autenticado");
        thenPhotoIsNotUpdated();
    }

    private UpdateStudentPhotoResponse whenUpdatingPhoto() {
        return updateStudentPhoto.execute(new UpdateStudentPhotoRequest(
                PARENT_COGNITO, ALUMNO_ID, NEW_PHOTO_URL));
    }

    private Alumno givenParentTutorAndStudentExist(boolean isStudentChildOfTutor) {
        Usuario parentUser = givenUserExistsWithRole(RolUsuario.PADRE);
        Tutor tutor = givenTutorExists(parentUser);
        Alumno alumno = givenStudentExists();

        if (isStudentChildOfTutor) {
            tutor.addHijo(alumno);
        }

        return alumno;
    }

    private void givenParentTutorAndStudentNotFound() {
        Usuario parentUser = givenUserExistsWithRole(RolUsuario.PADRE);
        givenTutorExists(parentUser);
        when(alumnoRepository.findById(ALUMNO_ID)).thenReturn(Optional.empty());
    }

    private Usuario givenUserExistsWithRole(RolUsuario rol) {
        Usuario parentUser = new Usuario();
        parentUser.setId(PARENT_ID);
        parentUser.setRol(rol);
        when(usuarioRepository.findByIdCognito(PARENT_COGNITO)).thenReturn(Optional.of(parentUser));
        return parentUser;
    }

    private void givenParentUserNotFound() {
        when(usuarioRepository.findByIdCognito(PARENT_COGNITO)).thenReturn(Optional.empty());
    }

    private Tutor givenTutorExists(Usuario parentUser) {
        Tutor tutor = new Tutor();
        tutor.setId(TUTOR_ID);
        tutor.setUsuario(parentUser);
        when(tutorRepository.findByUsuarioId(PARENT_ID)).thenReturn(Optional.of(tutor));
        return tutor;
    }

    private void givenTutorNotFound() {
        when(tutorRepository.findByUsuarioId(PARENT_ID)).thenReturn(Optional.empty());
    }

    private Alumno givenStudentExists() {
        Usuario studentUser = new Usuario();
        studentUser.setId(UUID.randomUUID());
        studentUser.setIdCognito(STUDENT_COGNITO);
        studentUser.setNombre("Lucas");
        studentUser.setApellido("Perez");

        Alumno alumno = new Alumno();
        alumno.setId(ALUMNO_ID);
        alumno.setUsuario(studentUser);

        when(alumnoRepository.findById(ALUMNO_ID)).thenReturn(Optional.of(alumno));
        return alumno;
    }

    private void thenPhotoIsUpdatedInRepository() {
        verify(usuarioRepository).updateFotoPerfil(STUDENT_COGNITO, NEW_PHOTO_URL);
    }

    private void thenResponseContainsUpdatedPhoto(UpdateStudentPhotoResponse response, Alumno alumno) {
        assertNotNull(response);
        assertEquals(ALUMNO_ID, response.id());
        assertEquals("Lucas", response.nombre());
        assertEquals("Perez", response.apellido());
        assertEquals(NEW_PHOTO_URL, response.urlFotoPerfil());
        assertEquals(NEW_PHOTO_URL, alumno.getUsuario().getUrlFotoPerfil());
    }

    private void thenThrowsIllegalArgumentExceptionWithMessage(Executable execution, String message) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);
        assertEquals(message, exception.getMessage());
    }

    private void thenThrowsOperationNotAllowedExceptionWithMessage(Executable execution, String message) {
        OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class, execution);
        assertEquals(message, exception.getMessage());
    }

    private void thenThrowsAlumnoNoEncontradoException(Executable execution, UUID id) {
        AlumnoNoEncontradoException exception = assertThrows(AlumnoNoEncontradoException.class, execution);
        assertEquals("No existe el alumno con id " + id, exception.getMessage());
    }

    private void thenNoFurtherActionIsTaken() {
        verify(tutorRepository, never()).findByUsuarioId(any());
        verify(alumnoRepository, never()).findById(any());
        verify(usuarioRepository, never()).updateFotoPerfil(anyString(), anyString());
    }

    private void thenNoStudentActionIsTaken() {
        verify(alumnoRepository, never()).findById(any());
        verify(usuarioRepository, never()).updateFotoPerfil(anyString(), anyString());
    }

    private void thenPhotoIsNotUpdated() {
        verify(usuarioRepository, never()).updateFotoPerfil(anyString(), anyString());
    }
}
