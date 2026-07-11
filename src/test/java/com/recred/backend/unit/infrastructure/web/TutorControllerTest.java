package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.CreateStudentFromParent;
import com.recred.backend.application.CreateStudentFromParentRequest;
import com.recred.backend.application.CreateStudentFromParentResponse;
import com.recred.backend.application.GetChildrenByTutor;
import com.recred.backend.application.GetChildrenByTutorRequest;
import com.recred.backend.application.GetChildrenByTutorResponse;
import com.recred.backend.application.GetTutorSchools;
import com.recred.backend.application.UpdateStudentPhoto;
import com.recred.backend.application.UpdateStudentPhotoRequest;
import com.recred.backend.application.UpdateStudentPhotoResponse;
import com.recred.backend.config.CustomAuthenticationEntryPoint;
import com.recred.backend.config.CustomJwtAuthenticationConverter;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.infrastructure.cloudinary.CloudinaryService;
import com.recred.backend.infrastructure.web.CreateChildRequestDTO;
import com.recred.backend.infrastructure.web.SchoolResponseDTO;
import com.recred.backend.infrastructure.web.StudentDTO;
import com.recred.backend.infrastructure.web.TutorController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class TutorControllerTest {

    private static final String TUTOR_SUB = "tutor-sub";

    @Mock
    private GetChildrenByTutor getChildrenByTutor;
    @Mock
    private CreateStudentFromParent createStudentFromParent;
    @Mock
    private GetTutorSchools getTutorSchools;
    @Mock
    private com.recred.backend.domain.services.UsuarioService usuarioService;
    @Mock
    private UpdateStudentPhoto updateStudentPhoto;
    @Mock
    private CloudinaryService cloudinaryService;

    private UUID tutorId;
    private UUID studentId;
    private UUID schoolId;
    private UUID gradeId;
    private ResponseEntity<List<StudentDTO>> childrenResponse;
    private ResponseEntity<StudentDTO> studentResponse;
    private ResponseEntity<List<SchoolResponseDTO>> schoolsResponse;
    private TutorController controller;
    private CreateChildRequestDTO createChildRequest;
    private MockMultipartFile photo;
    private MultipartFile unreadablePhoto;
    private String photoUrl;
    private UpdateStudentPhotoRequest updatePhotoRequest;
    private RuntimeException thrownRuntimeException;

    @BeforeEach
    void setUp() {
        givenController();
    }

    @Test
    void givenTutorWithChildrenWhenGettingChildrenByTutorIdThenReturnsStudents() {
        givenTutorWithChildren();

        whenGetChildrenByTutorId();

        thenStudentsAreReturned();
    }

    @Test
    void givenAuthenticatedTutorWhenGettingMyChildrenThenUsesCognitoSubject() {
        givenAuthenticatedTutorWithChildren();

        whenGetMyChildren();

        thenMyChildrenAreReturned();
        thenGetChildrenByTutorWasCalledWithCognitoSubject();
    }

    @Test
    void givenAuthenticatedTutorWhenCreatingChildThenReturnsCreated() {
        givenValidCreateChildRequest();
        givenCreateStudentFromParentReturnsCreatedChild();

        whenCreateChild();

        thenCreatedChildIsReturned();
        thenCreateStudentFromParentWasCalled();
    }

    @Test
    void givenAuthenticatedTutorWhenGettingMySchoolsThenReturnsAssociatedSchools() {
        givenTutorWithAssociatedSchools();

        whenGetMySchools();

        thenAssociatedSchoolsAreReturned();
        thenGetTutorSchoolsWasCalled();
    }

    @Test
    void givenTutorMeWithCurrentTutorWhenGettingChildrenThenUsesCurrentTutor() {
        givenCurrentUserHasTutor();
        givenCurrentTutorHasChildren();

        whenGetChildrenForMe();

        thenCurrentTutorChildrenAreReturned();
        thenCurrentTutorWasResolved();
    }

    @Test
    void givenTutorMeWithoutTutorProfileWhenGettingChildrenThenReturnsEmptyList() {
        givenCurrentUserWithoutTutor();

        whenGetChildrenForMe();

        thenChildrenResponseIsEmpty();
    }

    @Test
    void givenMissingAuthenticationWhenCreatingChildThenThrowsCredentialsException() {
        givenDirectCreateChildRequest();

        thenCreatingChildWithoutAuthenticationThrowsCredentialsException();
    }

    @Test
    void givenPhotoUploadWhenUpdatingChildPhotoThenUploadsAndUpdatesStudent() {
        givenPhotoUpload();
        givenUpdateStudentPhotoReturnsUpdatedStudent();

        whenUploadChildPhoto();

        thenUpdatedStudentPhotoIsReturned();
        thenPhotoWasUploadedAndStudentWasUpdated();
    }

    @Test
    void givenUnreadablePhotoWhenUpdatingChildPhotoThenThrowsRuntimeException() throws Exception {
        givenUnreadablePhoto();

        whenUploadUnreadableChildPhoto();

        thenPhotoReadErrorIsThrown();
        thenPhotoUploadWasNotStarted();
    }

    private void givenTutorWithChildren() {
        tutorId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        schoolId = UUID.randomUUID();
        GetChildrenByTutorResponse childResponse = childResponse(
                studentId,
                "Lucas",
                "Perez",
                "1 A",
                schoolId,
                "150.50",
                null
        );
        when(getChildrenByTutor.execute(GetChildrenByTutorRequest.byTutorId(tutorId)))
                .thenReturn(List.of(childResponse));
    }

    private void givenAuthenticatedTutorWithChildren() {
        studentId = UUID.randomUUID();
        GetChildrenByTutorResponse childResponse = childResponse(
                studentId,
                "Ana",
                "Lopez",
                null,
                null,
                "0",
                null
        );
        when(getChildrenByTutor.execute(GetChildrenByTutorRequest.byCognitoSubject(TUTOR_SUB)))
                .thenReturn(List.of(childResponse));
    }

    private void givenValidCreateChildRequest() {
        studentId = UUID.randomUUID();
        gradeId = UUID.randomUUID();
        createChildRequest = new CreateChildRequestDTO(
                "mateo",
                "Mateo",
                "Gomez",
                "mateo@test.com",
                "12345678",
                gradeId,
                null
        );
    }


    private void givenCreateStudentFromParentReturnsCreatedChild() {
        CreateStudentFromParentResponse createdChild = new CreateStudentFromParentResponse(
                studentId,
                "Mateo",
                "Gomez",
                true,
                null,
                null,
                null,
                BigDecimal.ZERO,
                null
        );
        when(createStudentFromParent.execute(expectedCreateStudentFromParentRequest()))
                .thenReturn(createdChild);
    }

    private void givenTutorWithAssociatedSchools() {
        schoolId = UUID.randomUUID();
        Colegio school = new Colegio();
        school.setId(schoolId);
        school.setNombre("Colegio Norte");
        when(getTutorSchools.execute(TUTOR_SUB)).thenReturn(List.of(school));
    }

    private void givenController() {
        controller = new TutorController(
                getChildrenByTutor,
                usuarioService,
                createStudentFromParent,
                updateStudentPhoto,
                cloudinaryService,
                getTutorSchools
        );
    }

    private void givenCurrentUserHasTutor() {
        tutorId = UUID.randomUUID();
        Usuario user = new Usuario();
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        user.setTutor(tutor);
        when(usuarioService.obtenerUsuarioActual(TUTOR_SUB)).thenReturn(user);
    }

    private void givenCurrentTutorHasChildren() {
        studentId = UUID.randomUUID();
        GetChildrenByTutorResponse childResponse = childResponse(
                studentId,
                "Sofia",
                "Diaz",
                null,
                null,
                "0",
                null
        );
        when(getChildrenByTutor.execute(GetChildrenByTutorRequest.byTutorId(tutorId)))
                .thenReturn(List.of(childResponse));
    }

    private void givenCurrentUserWithoutTutor() {
        when(usuarioService.obtenerUsuarioActual(TUTOR_SUB)).thenReturn(new Usuario());
    }

    private void givenDirectCreateChildRequest() {
        createChildRequest = new CreateChildRequestDTO(
                "mateo",
                "Mateo",
                "Gomez",
                "mateo@test.com",
                "12345678",
                UUID.randomUUID(),
                null
        );
    }

    private void givenPhotoUpload() {
        studentId = UUID.randomUUID();
        schoolId = UUID.randomUUID();
        photoUrl = "https://cdn.test/avatar.png";
        photo = new MockMultipartFile(
                "foto",
                "avatar.png",
                "image/png",
                "imagen".getBytes()
        );
        updatePhotoRequest = new UpdateStudentPhotoRequest(TUTOR_SUB, studentId, photoUrl);
        when(cloudinaryService.uploadImage(any(byte[].class), eq("avatar.png")))
                .thenReturn(photoUrl);
    }

    private void givenUpdateStudentPhotoReturnsUpdatedStudent() {
        UpdateStudentPhotoResponse updateResponse = new UpdateStudentPhotoResponse(
                studentId,
                "Sofia",
                "Diaz",
                true,
                "2 B",
                schoolId,
                new BigDecimal("25.00"),
                photoUrl
        );
        when(updateStudentPhoto.execute(updatePhotoRequest)).thenReturn(updateResponse);
    }

    private void givenUnreadablePhoto() throws IOException {
        studentId = UUID.randomUUID();
        unreadablePhoto = mock(MultipartFile.class);
        when(unreadablePhoto.getBytes()).thenThrow(new IOException("boom"));
    }

    private void whenGetChildrenByTutorId() {
        childrenResponse = controller.obtenerHijos(
                tutorId.toString(),
                new TestingAuthenticationToken(TUTOR_SUB, "")
        );
    }

    private void whenGetMyChildren() {
        childrenResponse = controller.obtenerMisHijos(
                new TestingAuthenticationToken(TUTOR_SUB, "")
        );
    }

    private void whenCreateChild() {
        studentResponse = controller.crearHijo(
                new TestingAuthenticationToken(TUTOR_SUB, ""),
                createChildRequest
        );
    }

    private void whenGetMySchools() {
        schoolsResponse = controller.obtenerMisColegios(new TestingAuthenticationToken(TUTOR_SUB, ""));
    }

    private void whenGetChildrenForMe() {
        childrenResponse = controller.obtenerHijos(
                "me",
                new TestingAuthenticationToken(TUTOR_SUB, "")
        );
    }

    private void whenUploadChildPhoto() {
        studentResponse = controller.subirFotoHijo(
                new TestingAuthenticationToken(TUTOR_SUB, ""),
                studentId,
                photo
        );
    }

    private void whenUploadUnreadableChildPhoto() {
        thrownRuntimeException = assertThrows(RuntimeException.class,
                () -> controller.subirFotoHijo(
                        new TestingAuthenticationToken(TUTOR_SUB, ""),
                        studentId,
                        unreadablePhoto
                ));
    }

    private void thenStudentsAreReturned() {
        assertEquals(200, childrenResponse.getStatusCode().value());
        assertEquals(studentId, childrenResponse.getBody().get(0).id());
        assertEquals("Lucas", childrenResponse.getBody().get(0).nombre());
        assertEquals("Perez", childrenResponse.getBody().get(0).apellido());
        assertEquals(true, childrenResponse.getBody().get(0).activo());
        assertEquals("1 A", childrenResponse.getBody().get(0).grado());
        assertEquals(schoolId, childrenResponse.getBody().get(0).colegioId());
        assertEquals(new BigDecimal("150.50"), childrenResponse.getBody().get(0).saldo());
    }

    private void thenMyChildrenAreReturned() {
        assertEquals(200, childrenResponse.getStatusCode().value());
        assertEquals(studentId, childrenResponse.getBody().get(0).id());
        assertEquals("Ana", childrenResponse.getBody().get(0).nombre());
    }

    private void thenGetChildrenByTutorWasCalledWithCognitoSubject() {
        verify(getChildrenByTutor).execute(GetChildrenByTutorRequest.byCognitoSubject(TUTOR_SUB));
    }

    private void thenCreatedChildIsReturned() {
        assertEquals(201, studentResponse.getStatusCode().value());
        assertEquals(studentId, studentResponse.getBody().id());
        assertEquals("Mateo", studentResponse.getBody().nombre());
    }


    private void thenCreateStudentFromParentWasCalled() {
        verify(createStudentFromParent).execute(expectedCreateStudentFromParentRequest());
    }

    private void thenCreateStudentFromParentWasNotCalled() {
        verifyNoInteractions(createStudentFromParent);
    }

    private void thenAssociatedSchoolsAreReturned() {
        assertEquals(200, schoolsResponse.getStatusCode().value());
        assertEquals(schoolId, schoolsResponse.getBody().get(0).id());
        assertEquals("Colegio Norte", schoolsResponse.getBody().get(0).nombre());
    }

    private void thenGetTutorSchoolsWasCalled() {
        verify(getTutorSchools).execute(TUTOR_SUB);
    }

    private void thenCurrentTutorChildrenAreReturned() {
        assertEquals(200, childrenResponse.getStatusCode().value());
        assertEquals(studentId, childrenResponse.getBody().get(0).id());
    }

    private void thenCurrentTutorWasResolved() {
        verify(usuarioService).obtenerUsuarioActual(TUTOR_SUB);
        verify(getChildrenByTutor).execute(GetChildrenByTutorRequest.byTutorId(tutorId));
    }

    private void thenChildrenResponseIsEmpty() {
        assertEquals(200, childrenResponse.getStatusCode().value());
        assertEquals(List.of(), childrenResponse.getBody());
    }

    private void thenCreatingChildWithoutAuthenticationThrowsCredentialsException() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.crearHijo(null, createChildRequest));
    }

    private void thenUpdatedStudentPhotoIsReturned() {
        assertEquals(200, studentResponse.getStatusCode().value());
        assertEquals(studentId, studentResponse.getBody().id());
        assertEquals("Sofia", studentResponse.getBody().nombre());
        assertEquals(photoUrl, studentResponse.getBody().urlFotoPerfil());
    }

    private void thenPhotoWasUploadedAndStudentWasUpdated() {
        verify(cloudinaryService).uploadImage(any(byte[].class), eq("avatar.png"));
        verify(updateStudentPhoto).execute(updatePhotoRequest);
    }

    private void thenPhotoReadErrorIsThrown() {
        assertEquals("Error al leer la imagen", thrownRuntimeException.getMessage());
    }

    private void thenPhotoUploadWasNotStarted() {
        verifyNoInteractions(cloudinaryService, updateStudentPhoto);
    }

    private CreateStudentFromParentRequest expectedCreateStudentFromParentRequest() {
        return new CreateStudentFromParentRequest(
                TUTOR_SUB,
                "mateo",
                "Mateo",
                "Gomez",
                "mateo@test.com",
                "12345678",
                gradeId,
                null
        );
    }

    private GetChildrenByTutorResponse childResponse(UUID id, String nombre, String apellido,
                                                     String grado, UUID colegioId, String saldo,
                                                     String urlFotoPerfil) {
        return new GetChildrenByTutorResponse(
                id,
                nombre,
                apellido,
                true,
                grado,
                colegioId,
                new BigDecimal(saldo),
                urlFotoPerfil
        );
    }
}
