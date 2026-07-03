package com.recred.backend.unit.application;

import com.recred.backend.application.CreateStudentFromParent;
import com.recred.backend.application.CreateStudentFromParentRequest;
import com.recred.backend.application.CreateStudentFromParentResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.TutorSchoolStatus;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.GradoRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.TutorSchoolRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private static final String STUDENT_SUB = "student-sub";
    private static final String DEFAULT_USERNAME = "juan";
    private static final String DEFAULT_NAME = "Juan";
    private static final String DEFAULT_LAST_NAME = "Perez";
    private static final String DEFAULT_EMAIL = "juan@example.com";
    private static final String TRIMMED_USERNAME = "juan.perez";
    private static final String TRIMMED_EMAIL = "juan.perez@example.com";
    private static final String STUDENT_DNI = "12345678";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private TutorSchoolRepository tutorSchoolRepository;
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
                tutorSchoolRepository,
                cognitoStudentIdentityService);
    }

    @Test
    void parentCreatesStudentUserAndLinksItToTutor() {
        SavedEntities savedEntities = savedEntities();
        Grado grado = grado(UUID.randomUUID());
        CreateStudentFromParentRequest request = validRequestWithGrade(grado.getId());

        givenParentWithTutor();
        givenEmailAvailable(TRIMMED_EMAIL);
        givenGradeExists(grado);
        givenTutorAssociatedToSchool(grado);
        givenCognitoCreatesStudent(TRIMMED_USERNAME, TRIMMED_EMAIL);
        givenEntitiesArePersistedWith(savedEntities);

        CreateStudentFromParentResponse result = createStudentFromParent.execute(request);

        verify(usuarioRepository).save(usuarioCaptor.capture());
        verify(alumnoRepository).save(alumnoCaptor.capture());
        verify(tutorRepository).save(tutorCaptor.capture());

        assertCreatedUser(usuarioCaptor.getValue());
        assertCreatedStudent(
                alumnoCaptor.getValue(),
                usuarioCaptor.getValue(),
                savedEntities.userId(),
                grado);
        assertLinkedTutorAndStudent(tutorCaptor.getValue(), alumnoCaptor.getValue());
        assertEquals(savedEntities.alumnoId(), result.id());
    }

    @Test
    void nonParentUserCannotCreateStudent() {
        givenAuthenticatedUserWithRole(RolUsuario.ALUMNO);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> createStudentFromParent.execute(defaultRequest()));

        assertEquals("El usuario autenticado no tiene rol PADRE", exception.getMessage());
        assertStopsBeforeExternalUserCreation();
    }

    @Test
    void missingAuthenticatedUserShouldThrowException() {
        when(usuarioRepository.findByIdCognito(PARENT_SUB)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> createStudentFromParent.execute(defaultRequest()));

        assertEquals("Usuario autenticado no encontrado", exception.getMessage());
        assertStopsBeforeExternalUserCreation();
    }

    @Test
    void missingTutorShouldThrowException() {
        givenAuthenticatedParentWithoutTutor();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> createStudentFromParent.execute(defaultRequest()));

        assertEquals("Tutor no encontrado para el usuario autenticado", exception.getMessage());
        assertStopsBeforeExternalUserCreation();
    }

    @Test
    void duplicatedEmailStopsBeforeCreatingCognitoUser() {
        givenParentWithTutor();
        givenDuplicatedEmail(DEFAULT_EMAIL);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> createStudentFromParent.execute(requestWithEmail(" " + DEFAULT_EMAIL + " ")));

        assertEquals("Ya existe un usuario con el email indicado", exception.getMessage());
        assertStopsBeforeExternalUserCreation();
    }

    @Test
    void missingGradeStopsBeforeCreatingCognitoUser() {
        UUID gradoId = UUID.randomUUID();

        givenParentWithTutor();
        givenEmailAvailable(DEFAULT_EMAIL);
        givenMissingGrade(gradoId);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> createStudentFromParent.execute(requestWithGrade(gradoId)));

        assertEquals("No se encontro el grado indicado", exception.getMessage());
        assertStopsBeforeExternalUserCreation();
    }

    @Test
    void tutorCannotCreateStudentInUnassociatedSchool() {
        Grado grado = grado(UUID.randomUUID());

        givenParentWithTutor();
        givenEmailAvailable(DEFAULT_EMAIL);
        givenGradeExists(grado);
        givenTutorNotAssociatedToSchool(grado);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> createStudentFromParent.execute(requestWithGrade(grado.getId())));

        assertEquals("El tutor no esta asociado al colegio del grado indicado",
                exception.getMessage());
        assertStopsBeforeExternalUserCreation();
    }

    @Test
    void emailLikeUsernameIsRejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> createStudentFromParent.execute(requestWithUsername(DEFAULT_EMAIL)));

        assertEquals("El username no puede tener formato de email", exception.getMessage());
        verify(usuarioRepository, never()).findByIdCognito(any());
        assertStopsBeforeExternalUserCreation();
    }

    @Test
    void blankRequiredFieldsAreRejected() {
        assertRequiredFieldIsRejected(requestWithName(" "),
                "El nombre del alumno es obligatorio");
        assertRequiredFieldIsRejected(requestWithLastName(" "),
                "El apellido del alumno es obligatorio");
        assertRequiredFieldIsRejected(requestWithEmail(" "),
                "El email del alumno es obligatorio");
    }

    @Test
    void blankOptionalUsernameAndDniShouldBeStoredAsNull() {
        SavedEntities savedEntities = savedEntities();
        CreateStudentFromParentRequest request = request(
                " ",
                " Juan ",
                " Perez ",
                " " + TRIMMED_EMAIL + " ",
                " ",
                null,
                null);

        givenParentWithTutor();
        givenEmailAvailable(TRIMMED_EMAIL);
        givenCognitoCreatesStudent(null, TRIMMED_EMAIL, "generated");
        givenEntitiesArePersistedWith(savedEntities);

        createStudentFromParent.execute(request);

        verify(usuarioRepository).save(usuarioCaptor.capture());
        verify(alumnoRepository).save(alumnoCaptor.capture());
        assertNull(usuarioCaptor.getValue().getDni());
        assertNull(alumnoCaptor.getValue().getGrado());
    }

    @Test
    void cognitoFailureStopsLocalPersistence() {
        givenParentWithTutor();
        givenEmailAvailable(DEFAULT_EMAIL);
        givenCognitoFails();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> createStudentFromParent.execute(defaultRequest()));

        assertEquals("No se pudo crear el usuario en Cognito", exception.getMessage());
        assertLocalEntitiesWereNotPersisted();
    }

    private Usuario user(UUID id, RolUsuario rol) {
        Usuario user = new Usuario();
        user.setId(id);
        user.setRol(rol);
        return user;
    }

    private Grado grado(UUID id) {
        Grado grado = new Grado();
        grado.setId(id);
        Colegio school = new Colegio();
        school.setId(UUID.randomUUID());
        grado.setColegio(school);
        return grado;
    }

    private SavedEntities savedEntities() {
        return new SavedEntities(UUID.randomUUID(), UUID.randomUUID());
    }

    private Tutor givenParentWithTutor() {
        UUID parentUserId = UUID.randomUUID();
        Usuario parent = user(parentUserId, RolUsuario.PADRE);
        Tutor tutor = tutor(UUID.randomUUID());

        when(usuarioRepository.findByIdCognito(PARENT_SUB)).thenReturn(Optional.of(parent));
        when(tutorRepository.findByUsuarioId(parentUserId)).thenReturn(Optional.of(tutor));
        return tutor;
    }

    private void givenAuthenticatedUserWithRole(RolUsuario rol) {
        Usuario user = user(UUID.randomUUID(), rol);
        when(usuarioRepository.findByIdCognito(PARENT_SUB)).thenReturn(Optional.of(user));
    }

    private void givenAuthenticatedParentWithoutTutor() {
        Usuario parent = user(UUID.randomUUID(), RolUsuario.PADRE);
        when(usuarioRepository.findByIdCognito(PARENT_SUB)).thenReturn(Optional.of(parent));
        when(tutorRepository.findByUsuarioId(parent.getId())).thenReturn(Optional.empty());
    }

    private void givenEmailAvailable(String email) {
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());
    }

    private void givenDuplicatedEmail(String email) {
        when(usuarioRepository.findByEmail(email))
                .thenReturn(Optional.of(user(UUID.randomUUID(), RolUsuario.ALUMNO)));
    }

    private void givenGradeExists(Grado grado) {
        when(gradoRepository.findById(grado.getId())).thenReturn(Optional.of(grado));
    }

    private void givenTutorAssociatedToSchool(Grado grado) {
        when(tutorSchoolRepository.existsByTutorIdAndSchoolIdAndStatus(
                any(),
                org.mockito.ArgumentMatchers.eq(grado.getColegio().getId()),
                org.mockito.ArgumentMatchers.eq(TutorSchoolStatus.ACTIVE)))
                .thenReturn(true);
    }

    private void givenTutorNotAssociatedToSchool(Grado grado) {
        when(tutorSchoolRepository.existsByTutorIdAndSchoolIdAndStatus(
                any(),
                org.mockito.ArgumentMatchers.eq(grado.getColegio().getId()),
                org.mockito.ArgumentMatchers.eq(TutorSchoolStatus.ACTIVE)))
                .thenReturn(false);
    }

    private void givenMissingGrade(UUID gradoId) {
        when(gradoRepository.findById(gradoId)).thenReturn(Optional.empty());
    }

    private void givenCognitoCreatesStudent(String username, String email) {
        givenCognitoCreatesStudent(username, email, username);
    }

    private void givenCognitoCreatesStudent(String username, String email,
                                            String generatedUsername) {
        when(cognitoStudentIdentityService.createStudentUser(
                username,
                email,
                DEFAULT_NAME,
                DEFAULT_LAST_NAME))
                .thenReturn(new CreatedCognitoUser(STUDENT_SUB, generatedUsername));
    }

    private void givenCognitoFails() {
        when(cognitoStudentIdentityService.createStudentUser(
                DEFAULT_USERNAME,
                DEFAULT_EMAIL,
                DEFAULT_NAME,
                DEFAULT_LAST_NAME))
                .thenThrow(new IllegalStateException(
                        "No se pudo crear el usuario en Cognito"));
    }

    private Tutor tutor(UUID id) {
        Tutor tutor = new Tutor();
        tutor.setId(id);
        return tutor;
    }

    private void givenEntitiesArePersistedWith(SavedEntities savedEntities) {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario saved = invocation.getArgument(0);
            saved.setId(savedEntities.userId());
            return saved;
        });
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(invocation -> {
            Alumno saved = invocation.getArgument(0);
            saved.setId(savedEntities.alumnoId());
            return saved;
        });
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(invocation ->
                invocation.getArgument(0));
    }

    private CreateStudentFromParentRequest defaultRequest() {
        return request(
                DEFAULT_USERNAME,
                DEFAULT_NAME,
                DEFAULT_LAST_NAME,
                DEFAULT_EMAIL,
                null,
                null,
                null);
    }

    private CreateStudentFromParentRequest validRequestWithGrade(UUID gradoId) {
        return request(
                " " + TRIMMED_USERNAME + " ",
                " Juan ",
                " Perez ",
                " " + TRIMMED_EMAIL + " ",
                " " + STUDENT_DNI + " ",
                gradoId,
                null);
    }

    private CreateStudentFromParentRequest requestWithUsername(String username) {
        return request(
                username,
                DEFAULT_NAME,
                DEFAULT_LAST_NAME,
                DEFAULT_EMAIL,
                null,
                null,
                null);
    }

    private CreateStudentFromParentRequest requestWithName(String nombre) {
        return request(
                DEFAULT_USERNAME,
                nombre,
                DEFAULT_LAST_NAME,
                DEFAULT_EMAIL,
                null,
                null,
                null);
    }

    private CreateStudentFromParentRequest requestWithLastName(String apellido) {
        return request(
                DEFAULT_USERNAME,
                DEFAULT_NAME,
                apellido,
                DEFAULT_EMAIL,
                null,
                null,
                null);
    }

    private CreateStudentFromParentRequest requestWithEmail(String email) {
        return request(
                DEFAULT_USERNAME,
                DEFAULT_NAME,
                DEFAULT_LAST_NAME,
                email,
                null,
                null,
                null);
    }

    private CreateStudentFromParentRequest requestWithGrade(UUID gradoId) {
        return request(
                DEFAULT_USERNAME,
                DEFAULT_NAME,
                DEFAULT_LAST_NAME,
                DEFAULT_EMAIL,
                null,
                gradoId,
                null);
    }

    private CreateStudentFromParentRequest request(String username, String nombre,
                                                   String apellido, String email, String dni,
                                                   UUID gradoId, String urlFotoPerfil) {
        return new CreateStudentFromParentRequest(
                PARENT_SUB,
                username,
                nombre,
                apellido,
                email,
                dni,
                gradoId,
                urlFotoPerfil
        );
    }

    private void assertRequiredFieldIsRejected(CreateStudentFromParentRequest request,
                                               String expectedMessage) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> createStudentFromParent.execute(request));

        assertEquals(expectedMessage, exception.getMessage());
        verify(usuarioRepository, never()).findByIdCognito(any());
        assertStopsBeforeExternalUserCreation();
    }

    private void assertCreatedUser(Usuario savedUser) {
        assertEquals(STUDENT_SUB, savedUser.getIdCognito());
        assertEquals(TRIMMED_EMAIL, savedUser.getEmail());
        assertEquals(DEFAULT_NAME, savedUser.getNombre());
        assertEquals(DEFAULT_LAST_NAME, savedUser.getApellido());
        assertEquals(STUDENT_DNI, savedUser.getDni());
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

    private void assertLinkedTutorAndStudent(Tutor tutor, Alumno student) {
        assertTrue(tutor.getHijos().contains(student));
        assertTrue(student.getTutores().contains(tutor));
    }

    private void assertExternalUserWasNotCreated() {
        verify(cognitoStudentIdentityService, never())
                .createStudentUser(any(), any(), any(), any());
    }

    private void assertStopsBeforeExternalUserCreation() {
        assertExternalUserWasNotCreated();
        assertLocalEntitiesWereNotPersisted();
    }

    private void assertLocalEntitiesWereNotPersisted() {
        verify(usuarioRepository, never()).save(any());
        verify(alumnoRepository, never()).save(any());
        verify(tutorRepository, never()).save(any());
    }

    private record SavedEntities(UUID userId, UUID alumnoId) {
    }
}
