package com.recred.backend.unit.application;

import com.recred.backend.application.CreateStudentFromParent;
import com.recred.backend.application.CreateStudentFromParentRequest;
import com.recred.backend.application.CreateStudentFromParentResponse;
import com.recred.backend.domain.exceptions.OperationNotAllowedException;
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
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private CreateStudentFromParentRequest createStudentRequest;
    private CreateStudentFromParentResponse createStudentResponse;
    private Throwable thrownException;
    private Grado grade;
    private UUID missingGradeId;
    private UUID savedUserId;
    private UUID savedStudentId;

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
    void givenValidStudentCreationScenario_whenCreatingStudent_thenCreatesUserAndLinksTutor() {
        givenParentCanCreateStudentWithGrade();
        givenStudentCanBeCreated();

        whenCreateStudentFromParent();

        thenStudentUserWasCreated();
        thenStudentWasCreatedWithGrade();
        thenStudentWasLinkedToTutor();
        thenResponseHasStudentId();
    }

    @Test
    void givenNonParentUser_whenCreatingStudent_thenThrowsException() {
        givenDefaultStudentCreationRequest();
        givenAuthenticatedUserWithRole(RolUsuario.ALUMNO);

        whenCreateStudentFails(OperationNotAllowedException.class);

        thenErrorMessageIs("El usuario autenticado no tiene rol PADRE");
        thenStudentCreationWasNotStarted();
    }

    @Test
    void givenMissingAuthenticatedUser_whenCreatingStudent_thenThrowsException() {
        givenDefaultStudentCreationRequest();
        givenAuthenticatedUserDoesNotExist();

        whenCreateStudentFails(IllegalArgumentException.class);

        thenErrorMessageIs("Usuario autenticado no encontrado");
        thenStudentCreationWasNotStarted();
    }

    @Test
    void givenParentWithoutTutorProfile_whenCreatingStudent_thenThrowsException() {
        givenDefaultStudentCreationRequest();
        givenAuthenticatedParentWithoutTutor();

        whenCreateStudentFails(IllegalArgumentException.class);

        thenErrorMessageIs("Tutor no encontrado para el usuario autenticado");
        thenStudentCreationWasNotStarted();
    }

    @Test
    void givenDuplicatedEmail_whenCreatingStudent_thenStopsBeforeCognito() {
        givenStudentCreationRequestWithEmail(" " + DEFAULT_EMAIL + " ");
        givenParentWithTutor();
        givenDuplicatedEmail(DEFAULT_EMAIL);

        whenCreateStudentFails(IllegalArgumentException.class);

        thenErrorMessageIs("Ya existe un usuario con el email indicado");
        thenStudentCreationWasNotStarted();
    }

    @Test
    void givenMissingGrade_whenCreatingStudent_thenStopsBeforeCognito() {
        givenStudentCreationRequestWithMissingGrade();
        givenParentWithTutor();
        givenEmailAvailable(DEFAULT_EMAIL);
        givenGradeDoesNotExist();

        whenCreateStudentFails(IllegalArgumentException.class);

        thenErrorMessageIs("No se encontro el grado indicado");
        thenStudentCreationWasNotStarted();
    }

    @Test
    void givenTutorNotAssociatedToSchool_whenCreatingStudent_thenThrowsException() {
        givenStudentCreationRequestWithGrade();
        givenParentWithTutor();
        givenEmailAvailable(DEFAULT_EMAIL);
        givenGradeExists();
        givenTutorNotAssociatedToSchool();

        whenCreateStudentFails(OperationNotAllowedException.class);

        thenErrorMessageIs("El tutor no esta asociado al colegio del grado indicado");
        thenStudentCreationWasNotStarted();
    }

    @Test
    void givenBlankOptionalFields_whenCreatingStudent_thenStoresOptionalFieldsAsNull() {
        givenStudentCreationRequestWithBlankOptionalFields();
        givenParentWithTutor();
        givenEmailAvailable(TRIMMED_EMAIL);
        givenCognitoCreatesStudent(null, TRIMMED_EMAIL, "generated");
        givenRepositoriesPersistCreatedEntities();

        whenCreateStudentFromParent();

        thenOptionalUsernameAndDniWereStoredAsNull();
    }

    @Test
    void givenCognitoFailure_whenCreatingStudent_thenStopsLocalPersistence() {
        givenDefaultStudentCreationRequest();
        givenParentWithTutor();
        givenEmailAvailable(DEFAULT_EMAIL);
        givenCognitoFails();

        whenCreateStudentFails(IllegalStateException.class);

        thenErrorMessageIs("No se pudo crear el usuario en Cognito");
        thenLocalEntitiesWereNotPersisted();
    }

    private Usuario user(UUID id, RolUsuario rol) {
        Usuario user = new Usuario();
        user.setId(id);
        user.setRol(rol);
        return user;
    }

    private Grado grade(UUID id) {
        Grado grado = new Grado();
        grado.setId(id);
        Colegio school = new Colegio();
        school.setId(randomUUID());
        grado.setColegio(school);
        return grado;
    }

    private void givenValidStudentCreationScenario() {
        givenPersistedIds();
        grade = grade(randomUUID());
        createStudentRequest = validRequestWithGrade(grade.getId());
    }

    private void givenParentCanCreateStudentWithGrade() {
        givenValidStudentCreationScenario();
        givenParentWithTutor();
        givenEmailAvailable(TRIMMED_EMAIL);
        givenGradeExists();
        givenTutorAssociatedToSchool();
    }

    private void givenStudentCanBeCreated() {
        givenCognitoCreatesStudent(TRIMMED_USERNAME, TRIMMED_EMAIL);
        givenRepositoriesPersistCreatedEntities();
    }

    private void givenDefaultStudentCreationRequest() {
        createStudentRequest = defaultRequest();
    }

    private void givenStudentCreationRequestWithEmail(String email) {
        createStudentRequest = requestWithEmail(email);
    }

    private void givenStudentCreationRequestWithMissingGrade() {
        missingGradeId = randomUUID();
        createStudentRequest = requestWithGrade(missingGradeId);
    }

    private void givenStudentCreationRequestWithGrade() {
        grade = grade(randomUUID());
        createStudentRequest = requestWithGrade(grade.getId());
    }

    private void givenStudentCreationRequestWithBlankOptionalFields() {
        givenPersistedIds();
        createStudentRequest = request(
                " ",
                " Juan ",
                " Perez ",
                " " + TRIMMED_EMAIL + " ",
                " ",
                null,
                null);
    }

    private void givenPersistedIds() {
        savedUserId = randomUUID();
        savedStudentId = randomUUID();
    }

    private void givenParentWithTutor() {
        UUID parentUserId = randomUUID();
        Usuario parent = user(parentUserId, RolUsuario.PADRE);
        Tutor tutor = tutor(randomUUID());

        when(usuarioRepository.findByIdCognito(PARENT_SUB)).thenReturn(Optional.of(parent));
        when(tutorRepository.findByUsuarioId(parentUserId)).thenReturn(Optional.of(tutor));
    }

    private void givenAuthenticatedUserDoesNotExist() {
        when(usuarioRepository.findByIdCognito(PARENT_SUB)).thenReturn(Optional.empty());
    }

    private void givenAuthenticatedUserWithRole(RolUsuario rol) {
        Usuario user = user(randomUUID(), rol);
        when(usuarioRepository.findByIdCognito(PARENT_SUB)).thenReturn(Optional.of(user));
    }

    private void givenAuthenticatedParentWithoutTutor() {
        Usuario parent = user(randomUUID(), RolUsuario.PADRE);
        when(usuarioRepository.findByIdCognito(PARENT_SUB)).thenReturn(Optional.of(parent));
        when(tutorRepository.findByUsuarioId(parent.getId())).thenReturn(Optional.empty());
    }

    private void givenEmailAvailable(String email) {
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());
    }

    private void givenDuplicatedEmail(String email) {
        when(usuarioRepository.findByEmail(email))
                .thenReturn(Optional.of(user(randomUUID(), RolUsuario.ALUMNO)));
    }

    private void givenGradeExists(Grado grado) {
        when(gradoRepository.findById(grado.getId())).thenReturn(Optional.of(grado));
    }

    private void givenGradeExists() {
        givenGradeExists(grade);
    }

    private void givenTutorAssociatedToSchool(Grado grado) {
        when(tutorSchoolRepository.existsByTutorIdAndSchoolIdAndStatus(
                any(),
                eq(grado.getColegio().getId()),
                eq(TutorSchoolStatus.ACTIVE)))
                .thenReturn(true);
    }

    private void givenTutorAssociatedToSchool() {
        givenTutorAssociatedToSchool(grade);
    }

    private void givenTutorNotAssociatedToSchool(Grado grado) {
        when(tutorSchoolRepository.existsByTutorIdAndSchoolIdAndStatus(
                any(),
                eq(grado.getColegio().getId()),
                eq(TutorSchoolStatus.ACTIVE)))
                .thenReturn(false);
    }

    private void givenTutorNotAssociatedToSchool() {
        givenTutorNotAssociatedToSchool(grade);
    }

    private void givenMissingGrade(UUID gradoId) {
        when(gradoRepository.findById(gradoId)).thenReturn(Optional.empty());
    }

    private void givenGradeDoesNotExist() {
        givenMissingGrade(missingGradeId);
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

    private void givenRepositoriesPersistCreatedEntitiesWith(UUID userId, UUID studentId) {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario saved = invocation.getArgument(0);
            saved.setId(userId);
            return saved;
        });
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(invocation -> {
            Alumno saved = invocation.getArgument(0);
            saved.setId(studentId);
            return saved;
        });
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(invocation ->
                invocation.getArgument(0));
    }

    private void givenRepositoriesPersistCreatedEntities() {
        givenRepositoriesPersistCreatedEntitiesWith(savedUserId, savedStudentId);
    }

    private void whenCreateStudentFromParent() {
        createStudentResponse = createStudentFromParent.execute(createStudentRequest);
    }

    private <T extends Throwable> void whenCreateStudentFails(Class<T> expectedException) {
        Executable execution = () -> createStudentFromParent.execute(createStudentRequest);
        thrownException = assertThrows(expectedException, execution);
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

    private void thenStudentUserWasCreated() {
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertCreatedUser(usuarioCaptor.getValue());
    }

    private void thenStudentWasCreatedWithGrade() {
        verify(alumnoRepository).save(alumnoCaptor.capture());
        assertCreatedStudent(
                alumnoCaptor.getValue(),
                usuarioCaptor.getValue(),
                savedUserId,
                grade);
    }

    private void thenStudentWasLinkedToTutor() {
        verify(tutorRepository).save(tutorCaptor.capture());
        assertLinkedTutorAndStudent(tutorCaptor.getValue(), alumnoCaptor.getValue());
    }

    private void thenResponseHasStudentId() {
        assertEquals(savedStudentId, createStudentResponse.id());
    }

    private void thenOptionalUsernameAndDniWereStoredAsNull() {
        verify(usuarioRepository).save(usuarioCaptor.capture());
        verify(alumnoRepository).save(alumnoCaptor.capture());
        assertNull(usuarioCaptor.getValue().getDni());
        assertNull(alumnoCaptor.getValue().getGrado());
    }

    private void thenErrorMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private void thenStudentCreationWasNotStarted() {
        thenExternalUserWasNotCreated();
        thenLocalEntitiesWereNotPersisted();
    }

    private void thenExternalUserWasNotCreated() {
        verify(cognitoStudentIdentityService, never())
                .createStudentUser(any(), any(), any(), any());
    }

    private void thenLocalEntitiesWereNotPersisted() {
        verify(usuarioRepository, never()).save(any());
        verify(alumnoRepository, never()).save(any());
        verify(tutorRepository, never()).save(any());
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
}
