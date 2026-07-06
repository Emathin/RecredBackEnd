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
import com.recred.backend.infrastructure.web.StudentDTO;
import com.recred.backend.infrastructure.web.TutorController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TutorController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
class TutorControllerTest {

    private static final String TUTOR_SUB = "tutor-sub";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetChildrenByTutor getChildrenByTutor;

    @MockitoBean
    private CreateStudentFromParent createStudentFromParent;

    @MockitoBean
    private GetTutorSchools getTutorSchools;

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

    private UUID tutorId;
    private UUID studentId;
    private UUID schoolId;
    private UUID gradeId;
    private String createChildRequestBody;
    private ResultActions response;
    private ResponseEntity<List<StudentDTO>> childrenResponse;
    private ResponseEntity<StudentDTO> studentResponse;
    private TutorController controller;
    private CreateChildRequestDTO createChildRequest;
    private MockMultipartFile photo;
    private MultipartFile unreadablePhoto;
    private String photoUrl;
    private UpdateStudentPhotoRequest updatePhotoRequest;
    private RuntimeException thrownRuntimeException;

    @Test
    void givenTutorWithChildren_whenGettingChildrenByTutorId_thenReturnsStudents() throws Exception {
        givenTutorWithChildren();

        whenGetChildrenByTutorId();

        thenStudentsAreReturned();
    }

    @Test
    void givenAuthenticatedTutor_whenGettingMyChildren_thenUsesCognitoSubject() throws Exception {
        givenAuthenticatedTutorWithChildren();

        whenGetMyChildren();

        thenMyChildrenAreReturned();
        thenGetChildrenByTutorWasCalledWithCognitoSubject();
    }

    @Test
    void givenAuthenticatedTutor_whenCreatingChild_thenReturnsCreated() throws Exception {
        givenValidCreateChildRequest();
        givenCreateStudentFromParentReturnsCreatedChild();

        whenCreateChild();

        thenCreatedChildIsReturned();
        thenCreateStudentFromParentWasCalled();
    }

    @Test
    void givenInvalidChildRequest_whenCreatingChild_thenReturnsBadRequest() throws Exception {
        givenInvalidCreateChildRequest();

        whenCreateChild();

        thenBadRequestIsReturned();
        thenCreateStudentFromParentWasNotCalled();
    }

    @Test
    void givenAuthenticatedTutor_whenGettingMySchools_thenReturnsAssociatedSchools()
            throws Exception {
        givenTutorWithAssociatedSchools();

        whenGetMySchools();

        thenAssociatedSchoolsAreReturned();
        thenGetTutorSchoolsWasCalled();
    }

    @Test
    void givenTutorMeWithCurrentTutor_whenGettingChildren_thenUsesCurrentTutor() {
        givenController();
        givenCurrentUserHasTutor();
        givenCurrentTutorHasChildren();

        whenGetChildrenForMe();

        thenCurrentTutorChildrenAreReturned();
        thenCurrentTutorWasResolved();
    }

    @Test
    void givenTutorMeWithoutTutorProfile_whenGettingChildren_thenReturnsEmptyList() {
        givenController();
        givenCurrentUserWithoutTutor();

        whenGetChildrenForMe();

        thenChildrenResponseIsEmpty();
    }

    @Test
    void givenMissingAuthentication_whenCreatingChild_thenThrowsCredentialsException() {
        givenController();
        givenDirectCreateChildRequest();

        thenCreatingChildWithoutAuthenticationThrowsCredentialsException();
    }

    @Test
    void givenPhotoUpload_whenUpdatingChildPhoto_thenUploadsAndUpdatesStudent() {
        givenController();
        givenPhotoUpload();
        givenUpdateStudentPhotoReturnsUpdatedStudent();

        whenUploadChildPhoto();

        thenUpdatedStudentPhotoIsReturned();
        thenPhotoWasUploadedAndStudentWasUpdated();
    }

    @Test
    void givenUnreadablePhoto_whenUpdatingChildPhoto_thenThrowsRuntimeException() throws Exception {
        givenController();
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
        createChildRequestBody = """
                {
                  "username": "mateo",
                  "nombre": "Mateo",
                  "apellido": "Gomez",
                  "email": "mateo@test.com",
                  "dni": "12345678",
                  "gradoId": "%s"
                }
                """.formatted(gradeId);
    }

    private void givenInvalidCreateChildRequest() {
        createChildRequestBody = """
                {
                  "username": "mateo@test.com",
                  "nombre": " ",
                  "apellido": "Gomez",
                  "email": "mateo@test.com"
                }
                """;
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

    private void whenGetChildrenByTutorId() throws Exception {
        response = mockMvc.perform(get("/api/v1/tutores/{tutorId}/hijos", tutorId)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private void whenGetMyChildren() throws Exception {
        response = mockMvc.perform(get("/api/v1/tutores/me/hijos")
                .principal(new TestingAuthenticationToken(TUTOR_SUB, ""))
                .contentType(MediaType.APPLICATION_JSON));
    }

    private void whenCreateChild() throws Exception {
        response = mockMvc.perform(post("/api/v1/tutores/me/hijos")
                .principal(new TestingAuthenticationToken(TUTOR_SUB, ""))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createChildRequestBody));
    }

    private void whenGetMySchools() throws Exception {
        response = mockMvc.perform(get("/api/v1/tutores/me/colegios")
                .principal(new TestingAuthenticationToken(TUTOR_SUB, ""))
                .contentType(MediaType.APPLICATION_JSON));
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

    private void thenStudentsAreReturned() throws Exception {
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(studentId.toString()))
                .andExpect(jsonPath("$[0].nombre").value("Lucas"))
                .andExpect(jsonPath("$[0].apellido").value("Perez"))
                .andExpect(jsonPath("$[0].activo").value(true))
                .andExpect(jsonPath("$[0].grado").value("1 A"))
                .andExpect(jsonPath("$[0].colegioId").value(schoolId.toString()))
                .andExpect(jsonPath("$[0].saldo").value(150.50));
    }

    private void thenMyChildrenAreReturned() throws Exception {
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(studentId.toString()))
                .andExpect(jsonPath("$[0].nombre").value("Ana"));
    }

    private void thenGetChildrenByTutorWasCalledWithCognitoSubject() {
        verify(getChildrenByTutor).execute(GetChildrenByTutorRequest.byCognitoSubject(TUTOR_SUB));
    }

    private void thenCreatedChildIsReturned() throws Exception {
        response
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(studentId.toString()))
                .andExpect(jsonPath("$.nombre").value("Mateo"));
    }

    private void thenBadRequestIsReturned() throws Exception {
        response.andExpect(status().isBadRequest());
    }

    private void thenCreateStudentFromParentWasCalled() {
        verify(createStudentFromParent).execute(expectedCreateStudentFromParentRequest());
    }

    private void thenCreateStudentFromParentWasNotCalled() {
        verifyNoInteractions(createStudentFromParent);
    }

    private void thenAssociatedSchoolsAreReturned() throws Exception {
        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(schoolId.toString()))
                .andExpect(jsonPath("$[0].nombre").value("Colegio Norte"));
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
