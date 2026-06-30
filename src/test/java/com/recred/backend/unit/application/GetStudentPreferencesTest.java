package com.recred.backend.unit.application;

import com.recred.backend.application.GetStudentPreferences;
import com.recred.backend.application.GetStudentPreferencesByUserRequest;
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

        GetStudentPreferencesResponse result =
                getStudentPreferences.executeByUserId(userRequest(
                        scenario.parentUserId(),
                        false,
                        TipoSugerenciaIA.PREFERENCIA_DETECTADA));

        assertParentPreferences(result, scenario);
        assertAcceptedAndSaved(scenario.firstLatest(), scenario.secondLatest());
        verify(sugerenciaIARepository, never()).guardar(scenario.firstOlder());
    }

    @Test
    void studentUserGetsLatestPendingSuggestionAndMarksItSeenByStudent() {
        StudentPreferenceScenario scenario = studentPreferenceScenario();
        givenStudentPreferenceScenario(scenario);

        GetStudentPreferencesResponse result =
                getStudentPreferences.executeByUserId(userRequest(
                        scenario.userId(),
                        true,
                        TipoSugerenciaIA.ALERTA_PRECIO));

        assertEquals(1, result.preferences().size());
        assertEquals(scenario.latest().getId(), result.preferences().get(0).suggestionId());
        assertEquals(scenario.alumno().getId(), result.preferences().get(0).studentId());
        assertTrue(!result.preferences().get(0).parentView());
        assertSeenByStudent(scenario.latest());
        verify(sugerenciaIARepository).guardar(scenario.latest());
        verify(sugerenciaIARepository, never()).guardar(scenario.older());
    }

    @Test
    void execute_WithTypeAndOnlyLast_ShouldReturnOnlyFirstPreference() {
        UUID alumnoId = randomUUID();
        SugerenciaIAEntity first = suggestion(TipoSugerenciaIA.PREFERENCIA_DETECTADA);
        SugerenciaIAEntity second = suggestion(TipoSugerenciaIA.PREFERENCIA_DETECTADA);
        when(sugerenciaIARepository.findByAlumnoIdAndTipo(
                alumnoId,
                TipoSugerenciaIA.PREFERENCIA_DETECTADA))
                .thenReturn(List.of(first, second));

        GetStudentPreferencesResponse result = getStudentPreferences.execute(request(
                alumnoId,
                true,
                TipoSugerenciaIA.PREFERENCIA_DETECTADA
        ));

        assertEquals(1, result.preferences().size());
        assertEquals(first.getId(), result.preferences().get(0).suggestionId());
    }

    @Test
    void execute_WithoutTypeAndWithoutOnlyLast_ShouldReturnAllPreferences() {
        UUID alumnoId = randomUUID();
        SugerenciaIAEntity first = suggestion(TipoSugerenciaIA.ALERTA_PRECIO);
        SugerenciaIAEntity second = suggestion(TipoSugerenciaIA.PREFERENCIA_DETECTADA);
        when(sugerenciaIARepository.findByAlumnoId(alumnoId)).thenReturn(List.of(first, second));

        GetStudentPreferencesResponse result = getStudentPreferences.execute(
                request(alumnoId, false, null));

        assertEquals(2, result.preferences().size());
        assertEquals(first.getId(), result.preferences().get(0).suggestionId());
        assertEquals(second.getId(), result.preferences().get(1).suggestionId());
    }

    @Test
    void execute_WithOnlyLastAndEmptyPreferences_ShouldReturnEmptyList() {
        UUID alumnoId = randomUUID();
        when(sugerenciaIARepository.findByAlumnoId(alumnoId)).thenReturn(List.of());

        GetStudentPreferencesResponse result = getStudentPreferences.execute(
                request(alumnoId, true, null));

        assertTrue(result.preferences().isEmpty());
    }

    @Test
    void executeByUserId_WhenParentChildrenHaveNoSuggestions_ShouldReturnEmptyList() {
        UUID parentUserId = randomUUID();
        Tutor tutor = new Tutor();
        tutor.setHijos(java.util.Set.of(child("Juan", "Perez")));

        when(tutorRepository.findByUsuarioId(parentUserId)).thenReturn(Optional.of(tutor));
        when(sugerenciaIARepository.findPendingParentNotificationsByAlumnoId(
                tutor.getHijos().iterator().next().getId(),
                null))
                .thenReturn(List.of());

        GetStudentPreferencesResponse result =
                getStudentPreferences.executeByUserId(userRequest(parentUserId, false, null));

        assertTrue(result.preferences().isEmpty());
    }

    @Test
    void executeByUserId_WhenStudentHasNoPendingSuggestion_ShouldReturnEmptyList() {
        UUID userId = randomUUID();
        UUID studentId = randomUUID();
        Alumno alumno = child("Juan", "Perez");
        alumno.setId(studentId);

        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.empty());
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(alumno));
        when(sugerenciaIARepository.findPendingStudentNotificationsByAlumnoId(
                studentId,
                null)).thenReturn(List.of());

        GetStudentPreferencesResponse result =
                getStudentPreferences.executeByUserId(userRequest(userId, false, null));

        assertTrue(result.preferences().isEmpty());
    }

    @Test
    void executeByUserId_WhenNoTutorOrStudentExists_ShouldThrowException() {
        UUID userId = randomUUID();
        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.empty());
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> getStudentPreferences.executeByUserId(userRequest(userId, false, null)));
    }

    @Test
    void executeByUserId_WhenStudentEntityDoesNotExist_ShouldThrowException() {
        UUID userId = randomUUID();
        UUID studentId = randomUUID();
        when(tutorRepository.findByUsuarioId(userId)).thenReturn(Optional.empty());
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> getStudentPreferences.executeByUserId(userRequest(userId, false, null)));
    }

    private GetStudentPreferencesRequest request(
            UUID alumnoId,
            boolean onlyLast,
            TipoSugerenciaIA type
    ) {
        return new GetStudentPreferencesRequest(alumnoId, onlyLast, type);
    }

    private GetStudentPreferencesByUserRequest userRequest(
            UUID userId,
            boolean onlyLast,
            TipoSugerenciaIA type
    ) {
        return new GetStudentPreferencesByUserRequest(userId, onlyLast, type);
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

    private void assertParentPreferences(
            GetStudentPreferencesResponse result,
            ParentPreferenceScenario scenario) {
        assertEquals(2, result.preferences().size());
        assertTrue(result.preferences().stream()
                .allMatch(GetStudentPreferencesResponse.Preference::parentView));
        assertContainsSuggestion(result, scenario.firstLatest());
        assertContainsSuggestion(result, scenario.secondLatest());
    }

    private void assertContainsSuggestion(
            GetStudentPreferencesResponse result,
            SugerenciaIAEntity suggestion) {
        assertTrue(result.preferences().stream()
                .map(GetStudentPreferencesResponse.Preference::suggestionId)
                .toList()
                .contains(suggestion.getId()));
    }

    private void assertAcceptedAndSaved(SugerenciaIAEntity... suggestions) {
        for (SugerenciaIAEntity suggestion : suggestions) {
            assertEquals(Boolean.TRUE, suggestion.getAceptada());
            assertNotNull(suggestion.getRespondidaEn());
            verify(sugerenciaIARepository).guardar(suggestion);
        }
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

    private void assertSeenByStudent(SugerenciaIAEntity suggestion) {
        assertEquals(Boolean.FALSE, suggestion.getAceptada());
        assertNotNull(suggestion.getRespondidaEn());
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
