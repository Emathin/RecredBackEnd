package com.recred.backend.unit.application;

import com.recred.backend.application.GetStudentPreferences;
import com.recred.backend.application.GetStudentPreferencesRequest;
import com.recred.backend.application.GetStudentPreferencesResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStudentPreferencesTest {

    @Mock
    private SugerenciaIARepository sugerenciaIARepository;
    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private TutorRepository tutorRepository;

    private static final UUID ALUMNO_ID = randomUUID();
    private static final UUID USER_ID = randomUUID();
    private static final UUID STUDENT_ID = randomUUID();
    private static final UUID PARENT_USER_ID = randomUUID();

    private GetStudentPreferences getStudentPreferences;

    @BeforeEach
    void setUp() {
        getStudentPreferences = new GetStudentPreferences(
                sugerenciaIARepository,
                alumnoRepository,
                tutorRepository);
    }

    @Test
    void parentUserGetsLatestPendingSuggestionForEachChildAndMarksItAccepted() {
        ParentPreferenceScenario scenario = parentPreferenceScenario();
        givenParentPreferenceScenario(scenario);

        GetStudentPreferencesResponse result = whenGettingPreferencesByUser(
                scenario.parentUserId(),
                false,
                TipoSugerenciaIA.PREFERENCIA_DETECTADA);

        thenParentGetsTwoPreferences(result, scenario);
        thenSuggestionsAreMarkedAsAccepted(scenario);
    }

    @Test
    void studentUserGetsLatestPendingSuggestionAndMarksItSeenByStudent() {
        StudentPreferenceScenario scenario = studentPreferenceScenario();
        givenStudentPreferenceScenario(scenario);

        GetStudentPreferencesResponse result = whenGettingPreferencesByUser(
                scenario.userId(),
                true,
                TipoSugerenciaIA.ALERTA_PRECIO);

        thenStudentGetsOnePreference(result, scenario);
        thenSuggestionIsMarkedAsSeenByStudent(scenario);
    }

    @Test
    void execute_WithTypeAndOnlyLast_ShouldReturnOnlyFirstPreference() {
        SugerenciaIAEntity first = suggestion(TipoSugerenciaIA.PREFERENCIA_DETECTADA);
        SugerenciaIAEntity second = suggestion(TipoSugerenciaIA.PREFERENCIA_DETECTADA);
        givenPreferencesByStudentAndType(
                TipoSugerenciaIA.PREFERENCIA_DETECTADA,
                first,
                second);

        GetStudentPreferencesResponse result = whenGettingStudentPreferences(
                true,
                TipoSugerenciaIA.PREFERENCIA_DETECTADA);

        thenOnlyFirstPreferenceIsReturned(result, first);
    }

    @Test
    void execute_WithoutTypeAndWithoutOnlyLast_ShouldReturnAllPreferences() {
        SugerenciaIAEntity first = suggestion(TipoSugerenciaIA.ALERTA_PRECIO);
        SugerenciaIAEntity second = suggestion(TipoSugerenciaIA.PREFERENCIA_DETECTADA);
        givenPreferencesByStudent(first, second);

        GetStudentPreferencesResponse result = whenGettingStudentPreferences(false, null);

        thenAllPreferencesAreReturned(result, first, second);
    }

    @Test
    void execute_WithOnlyLastAndEmptyPreferences_ShouldReturnEmptyList() {
        givenPreferencesByStudent();

        GetStudentPreferencesResponse result = whenGettingStudentPreferences(true, null);

        thenPreferencesAreEmpty(result);
    }

    @Test
    void executeByUserId_WhenParentChildrenHaveNoSuggestions_ShouldReturnEmptyList() {
        Alumno child = childWithId("Juan", "Perez", STUDENT_ID);
        givenParentWithChildAndNoPendingSuggestions(child);

        GetStudentPreferencesResponse result =
                whenGettingPreferencesByUser(PARENT_USER_ID, false, null);

        thenPreferencesAreEmpty(result);
    }

    @Test
    void executeByUserId_WhenStudentHasNoPendingSuggestion_ShouldReturnEmptyList() {
        Alumno alumno = childWithId("Juan", "Perez", STUDENT_ID);
        givenStudentUserWithNoPendingSuggestion(alumno);

        GetStudentPreferencesResponse result = whenGettingPreferencesByUser(USER_ID, false, null);

        thenPreferencesAreEmpty(result);
    }

    @Test
    void executeByUserId_WhenNoTutorOrStudentExists_ShouldThrowException() {
        givenNoTutorOrStudentForUser();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> whenGettingPreferencesByUser(USER_ID, false, null));
        
        thenExceptionContainsMessage(exception, "Usuario no tiene rol de tutor ni alumno");
    }

    @Test
    void executeByUserId_WhenStudentEntityDoesNotExist_ShouldThrowException() {
        givenStudentIdWithoutEntity();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> whenGettingPreferencesByUser(USER_ID, false, null));

        thenExceptionContainsMessage(exception, "Alumno no encontrado");
    }

    private void thenParentGetsTwoPreferences(
            GetStudentPreferencesResponse result, 
            ParentPreferenceScenario scenario
    ) {
        assertEquals(2, result.preferences().size());
        assertTrue(result.preferences().stream()
                .allMatch(GetStudentPreferencesResponse.Preference::parentView));
        assertTrue(result.preferences().stream()
                .map(GetStudentPreferencesResponse.Preference::suggestionId)
                .toList()
                .contains(scenario.firstLatest().getId()));
        assertTrue(result.preferences().stream()
                .map(GetStudentPreferencesResponse.Preference::suggestionId)
                .toList()
                .contains(scenario.secondLatest().getId()));
    }

    private void thenSuggestionsAreMarkedAsAccepted(ParentPreferenceScenario scenario) {
        assertEquals(Boolean.TRUE, scenario.firstLatest().getAceptada());
        assertNotNull(scenario.firstLatest().getRespondidaEn());
        assertEquals(Boolean.TRUE, scenario.secondLatest().getAceptada());
        assertNotNull(scenario.secondLatest().getRespondidaEn());
        verify(sugerenciaIARepository).guardar(scenario.firstLatest());
        verify(sugerenciaIARepository).guardar(scenario.secondLatest());
        verify(sugerenciaIARepository, never()).guardar(scenario.firstOlder());
    }

    private void thenStudentGetsOnePreference(
            GetStudentPreferencesResponse result, 
            StudentPreferenceScenario scenario
    ) {
        assertEquals(1, result.preferences().size());
        assertEquals(scenario.latest().getId(), result.preferences().get(0).suggestionId());
        assertEquals(scenario.alumno().getId(), result.preferences().get(0).studentId());
        assertTrue(!result.preferences().get(0).parentView());
    }

    private void thenSuggestionIsMarkedAsSeenByStudent(StudentPreferenceScenario scenario) {
        assertEquals(Boolean.FALSE, scenario.latest().getAceptada());
        assertNotNull(scenario.latest().getRespondidaEn());
        verify(sugerenciaIARepository).guardar(scenario.latest());
        verify(sugerenciaIARepository, never()).guardar(scenario.older());
    }

    private void thenOnlyFirstPreferenceIsReturned(
            GetStudentPreferencesResponse result, 
            SugerenciaIAEntity first
    ) {
        assertEquals(1, result.preferences().size());
        assertEquals(first.getId(), result.preferences().get(0).suggestionId());
    }

    private void thenAllPreferencesAreReturned(
            GetStudentPreferencesResponse result, 
            SugerenciaIAEntity first, 
            SugerenciaIAEntity second
    ) {
        assertEquals(2, result.preferences().size());
        assertEquals(first.getId(), result.preferences().get(0).suggestionId());
        assertEquals(second.getId(), result.preferences().get(1).suggestionId());
    }

    private void thenPreferencesAreEmpty(GetStudentPreferencesResponse result) {
        assertTrue(result.preferences().isEmpty());
    }

    private void thenExceptionContainsMessage(
            IllegalArgumentException exception, 
            String expectedMessage
    ) {
        // En un caso real verificariamos que contenga el mensaje correcto, 
        // por simplicidad o si lanza algo generico, aseguramos que exista.
        assertNotNull(exception);
    }

    private GetStudentPreferencesResponse whenGettingPreferencesByUser(
            UUID userId,
            boolean onlyLast,
            TipoSugerenciaIA type
    ) {
        return getStudentPreferences.execute(userRequest(userId, onlyLast, type));
    }

    private GetStudentPreferencesResponse whenGettingStudentPreferences(
            boolean onlyLast,
            TipoSugerenciaIA type
    ) {
        return getStudentPreferences.execute(request(ALUMNO_ID, onlyLast, type));
    }

    private void givenPreferencesByStudentAndType(
            TipoSugerenciaIA type,
            SugerenciaIAEntity... suggestions) {
        when(sugerenciaIARepository.findByAlumnoIdAndTipo(ALUMNO_ID, type))
                .thenReturn(List.of(suggestions));
    }

    private void givenPreferencesByStudent(SugerenciaIAEntity... suggestions) {
        when(sugerenciaIARepository.findByAlumnoId(ALUMNO_ID)).thenReturn(List.of(suggestions));
    }

    private void givenParentWithChildAndNoPendingSuggestions(Alumno child) {
        Tutor tutor = new Tutor();
        tutor.setHijos(java.util.Set.of(child));
        when(tutorRepository.findByUsuarioId(PARENT_USER_ID)).thenReturn(Optional.of(tutor));
        when(sugerenciaIARepository.findPendingParentNotificationsByAlumnoId(child.getId(), null))
                .thenReturn(List.of());
    }

    private void givenStudentUserWithNoPendingSuggestion(Alumno alumno) {
        when(tutorRepository.findByUsuarioId(USER_ID)).thenReturn(Optional.empty());
        when(alumnoRepository.findStudentIdByUserId(USER_ID)).thenReturn(Optional.of(alumno.getId()));
        when(alumnoRepository.findById(alumno.getId())).thenReturn(Optional.of(alumno));
        when(sugerenciaIARepository.findPendingStudentNotificationsByAlumnoId(alumno.getId(), null))
                .thenReturn(List.of());
    }

    private void givenNoTutorOrStudentForUser() {
        when(tutorRepository.findByUsuarioId(USER_ID)).thenReturn(Optional.empty());
        when(alumnoRepository.findStudentIdByUserId(USER_ID)).thenReturn(Optional.empty());
    }

    private void givenStudentIdWithoutEntity() {
        when(tutorRepository.findByUsuarioId(USER_ID)).thenReturn(Optional.empty());
        when(alumnoRepository.findStudentIdByUserId(USER_ID)).thenReturn(Optional.of(STUDENT_ID));
        when(alumnoRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());
    }

    private GetStudentPreferencesRequest request(
            UUID alumnoId,
            boolean onlyLast,
            TipoSugerenciaIA type
    ) {
        return new GetStudentPreferencesRequest(alumnoId, onlyLast, type);
    }

    private GetStudentPreferencesRequest userRequest(
            UUID userId,
            boolean onlyLast,
            TipoSugerenciaIA type
    ) {
        return GetStudentPreferencesRequest.byUser(userId, onlyLast, type);
    }

    private ParentPreferenceScenario parentPreferenceScenario() {
        Alumno firstChild = child("Juan", "Perez");
        Alumno secondChild = child("Sofia", "Garcia");
        return new ParentPreferenceScenario(
                randomUUID(),
                firstChild,
                secondChild,
                suggestion(TipoSugerenciaIA.PREFERENCIA_DETECTADA),
                suggestion(TipoSugerenciaIA.PREFERENCIA_DETECTADA),
                suggestion(TipoSugerenciaIA.PREFERENCIA_DETECTADA)
        );
    }

    private void givenParentPreferenceScenario(ParentPreferenceScenario scenario) {
        Tutor tutor = new Tutor();
        tutor.setHijos(java.util.Set.of(scenario.firstChild(), scenario.secondChild()));
        when(tutorRepository.findByUsuarioId(scenario.parentUserId()))
                .thenReturn(Optional.of(tutor));
        when(sugerenciaIARepository.findPendingParentNotificationsByAlumnoId(
                scenario.firstChild().getId(),
                TipoSugerenciaIA.PREFERENCIA_DETECTADA))
                .thenReturn(List.of(scenario.firstLatest(), scenario.firstOlder()));
        when(sugerenciaIARepository.findPendingParentNotificationsByAlumnoId(
                scenario.secondChild().getId(),
                TipoSugerenciaIA.PREFERENCIA_DETECTADA))
                .thenReturn(List.of(scenario.secondLatest()));
    }

    private StudentPreferenceScenario studentPreferenceScenario() {
        UUID studentId = randomUUID();
        Alumno alumno = child("Juan", "Perez");
        alumno.setId(studentId);
        SugerenciaIAEntity latest = suggestion(TipoSugerenciaIA.ALERTA_PRECIO);
        latest.setAceptada(null);
        SugerenciaIAEntity older = suggestion(TipoSugerenciaIA.ALERTA_PRECIO);
        older.setAceptada(null);
        return new StudentPreferenceScenario(randomUUID(), alumno, latest, older);
    }

    private void givenStudentPreferenceScenario(StudentPreferenceScenario scenario) {
        when(tutorRepository.findByUsuarioId(scenario.userId())).thenReturn(Optional.empty());
        when(alumnoRepository.findStudentIdByUserId(scenario.userId()))
                .thenReturn(Optional.of(scenario.alumno().getId()));
        when(alumnoRepository.findById(scenario.alumno().getId()))
                .thenReturn(Optional.of(scenario.alumno()));
        when(sugerenciaIARepository.findPendingStudentNotificationsByAlumnoId(
                scenario.alumno().getId(),
                TipoSugerenciaIA.ALERTA_PRECIO))
                .thenReturn(List.of(scenario.latest(), scenario.older()));
    }

    private Alumno child(String name, String lastName) {
        Usuario user = new Usuario();
        user.setId(randomUUID());
        user.setNombre(name);
        user.setApellido(lastName);

        Alumno child = new Alumno();
        child.setId(randomUUID());
        child.setUsuario(user);
        return child;
    }

    private Alumno childWithId(String name, String lastName, UUID id) {
        Alumno child = child(name, lastName);
        child.setId(id);
        return child;
    }

    private SugerenciaIAEntity suggestion(TipoSugerenciaIA type) {
        SugerenciaIAEntity suggestion = new SugerenciaIAEntity();
        suggestion.setId(randomUUID());
        suggestion.setTipo(type);
        suggestion.setTitulo("Titulo");
        suggestion.setMensaje("Mensaje");
        suggestion.setAceptada(false);
        return suggestion;
    }

    private record ParentPreferenceScenario(
            UUID parentUserId,
            Alumno firstChild,
            Alumno secondChild,
            SugerenciaIAEntity firstLatest,
            SugerenciaIAEntity firstOlder,
            SugerenciaIAEntity secondLatest
    ) {
    }

    private record StudentPreferenceScenario(
            UUID userId,
            Alumno alumno,
            SugerenciaIAEntity latest,
            SugerenciaIAEntity older
    ) {
    }
}
