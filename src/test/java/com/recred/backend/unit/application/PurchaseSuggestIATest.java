package com.recred.backend.unit.application;

import com.recred.backend.application.PurchaseSuggestIA;
import com.recred.backend.application.PurchaseSuggestIARequest;
import com.recred.backend.application.PurchaseSuggestionResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.domain.services.UsuarioService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PurchaseSuggestIATest {

    private static final String COGNITO_ID = "user-123";

    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final SugerenciaIARepository sugerenciaIARepository =
            mock(SugerenciaIARepository.class);

    private PurchaseSuggestIA useCase;
    private Usuario user;
    private Alumno student;
    private SugerenciaIA suggestion;
    private Optional<PurchaseSuggestionResponse> response;

    @Test
    void execute_WhenParentIsInVisibilityWindow_ReturnsLatestPendingSuggestion() {
        givenCurrentTimeIs(LocalDateTime.of(2026, 6, 1, 20, 5));
        givenParentWithStudent();
        givenLatestPendingHealthySuggestion(suggestion());

        whenSuggestingPurchase();

        thenSuggestionWasReturned(suggestion.getId());
        thenHealthySuggestionsWereRequestedFor(student.getId());
    }

    @Test
    void execute_WhenParentIsBeforeVisibilityWindow_ReturnsEmpty() {
        givenCurrentTimeIs(LocalDateTime.of(2026, 6, 1, 10, 0));
        givenParent();

        whenSuggestingPurchase();

        thenNoSuggestionWasReturned();
        thenSuggestionsWereNotRequested();
    }

    @Test
    void execute_WhenParentIsOnWeekend_ReturnsEmpty() {
        givenCurrentTimeIs(LocalDateTime.of(2026, 6, 6, 21, 0));
        givenParent();

        whenSuggestingPurchase();

        thenNoSuggestionWasReturned();
        thenSuggestionsWereNotRequested();
    }

    @Test
    void execute_WhenStudentHasAssociatedStudent_ReturnsSuggestion() {
        givenCurrentTimeIs(LocalDateTime.of(2026, 6, 1, 10, 0));
        givenStudentUser();
        givenLatestPendingHealthySuggestion(suggestion());

        whenSuggestingPurchase();

        thenSuggestionWasReturned(suggestion.getId());
        thenHealthySuggestionsWereRequestedFor(student.getId());
    }

    @Test
    void execute_WhenStudentIsOutsideSchoolHours_StillReturnsSuggestion() {
        givenCurrentTimeIs(LocalDateTime.of(2026, 6, 1, 15, 0));
        givenStudentUser();
        givenLatestPendingHealthySuggestion(suggestion());

        whenSuggestingPurchase();

        thenSuggestionWasReturned(suggestion.getId());
    }

    @Test
    void execute_WhenUserRoleIsNotVisible_ReturnsEmpty() {
        givenCurrentTimeIs(LocalDateTime.of(2026, 6, 1, 21, 0));
        givenUserWithRole(RolUsuario.VENDEDOR);

        whenSuggestingPurchase();

        thenNoSuggestionWasReturned();
        thenSuggestionsWereNotRequested();
    }

    @Test
    void execute_WhenVisibleParentHasNoTutor_ReturnsEmpty() {
        givenCurrentTimeIs(LocalDateTime.of(2026, 6, 1, 21, 0));
        givenParent();

        whenSuggestingPurchase();

        thenNoSuggestionWasReturned();
        thenSuggestionsWereNotRequested();
    }

    @Test
    void execute_WhenStudentUserHasNoStudent_ReturnsEmpty() {
        givenCurrentTimeIs(LocalDateTime.of(2026, 6, 1, 10, 0));
        givenUserWithRole(RolUsuario.ALUMNO);

        whenSuggestingPurchase();

        thenNoSuggestionWasReturned();
        thenSuggestionsWereNotRequested();
    }

    @Test
    void execute_WhenRepositoryFindsLatestPendingSuggestion_ReturnsIt() {
        givenCurrentTimeIs(LocalDateTime.of(2026, 6, 1, 21, 0));
        givenParentWithStudent();
        SugerenciaIA latestPending = suggestion();
        givenLatestPendingHealthySuggestion(latestPending);

        whenSuggestingPurchase();

        thenSuggestionWasReturned(latestPending.getId());
    }

    private void givenCurrentTimeIs(LocalDateTime currentTime) {
        Clock clock = Clock.fixed(
                currentTime.atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );
        useCase = new PurchaseSuggestIA(usuarioService, sugerenciaIARepository, clock);
    }

    private void givenParentWithStudent() {
        givenParent();
        student = student();
        Tutor tutor = new Tutor();
        tutor.addHijo(student);
        user.setTutor(tutor);
    }

    private void givenParent() {
        givenUserWithRole(RolUsuario.PADRE);
    }

    private void givenStudentUser() {
        givenUserWithRole(RolUsuario.ALUMNO);
        student = student();
        user.setAlumno(student);
    }

    private void givenUserWithRole(RolUsuario role) {
        user = new Usuario();
        user.setRol(role);
        when(usuarioService.obtenerUsuarioActual(COGNITO_ID)).thenReturn(user);
    }

    private void givenLatestPendingHealthySuggestion(SugerenciaIA latestSuggestion) {
        suggestion = latestSuggestion;
        when(sugerenciaIARepository.findLatestPendingByAlumnoIdsAndTipo(
                List.of(student.getId()),
                TipoSugerenciaIA.RECOMENDACION_SALUDABLE
        )).thenReturn(Optional.of(latestSuggestion));
    }

    private void whenSuggestingPurchase() {
        response = useCase.execute(new PurchaseSuggestIARequest(COGNITO_ID));
    }

    private void thenSuggestionWasReturned(UUID expectedSuggestionId) {
        assertTrue(response.isPresent());
        assertEquals(expectedSuggestionId, response.get().id());
    }

    private void thenNoSuggestionWasReturned() {
        assertFalse(response.isPresent());
    }

    private void thenHealthySuggestionsWereRequestedFor(UUID expectedStudentId) {
        verify(sugerenciaIARepository).findLatestPendingByAlumnoIdsAndTipo(
                List.of(expectedStudentId),
                TipoSugerenciaIA.RECOMENDACION_SALUDABLE
        );
    }

    private void thenSuggestionsWereNotRequested() {
        verifyNoInteractions(sugerenciaIARepository);
    }

    private Alumno student() {
        Alumno student = new Alumno();
        student.setId(randomUUID());
        return student;
    }

    private SugerenciaIA suggestion() {
        return SugerenciaIA.builder()
                .id(randomUUID())
                .tipo(TipoSugerenciaIA.RECOMENDACION_SALUDABLE)
                .titulo("Test")
                .mensaje("Mensaje")
                .build();
    }
}
