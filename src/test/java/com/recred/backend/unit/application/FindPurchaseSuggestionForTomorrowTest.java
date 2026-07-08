package com.recred.backend.unit.application;

import com.recred.backend.application.FindPurchaseSuggestionForTomorrow;
import com.recred.backend.application.FindPurchaseSuggestionRequest;
import com.recred.backend.application.FindPurchaseSuggestionResponse;
import com.recred.backend.domain.models.TipoNotificacion;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.SuggestionAiRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindPurchaseSuggestionForTomorrowTest {

    private static final String COGNITO_SUBJECT = "test-sub";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-06T10:00:00Z"),
            ZoneId.of("UTC")
    );
    private static final String TOMORROW_DAY = "TUESDAY";

    @Mock
    private SuggestionAiRepository suggestionAiRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AlumnoRepository alumnoRepository;

    private FindPurchaseSuggestionForTomorrow findPurchaseSuggestionForTomorrow;
    private FindPurchaseSuggestionRequest suggestionRequest;
    private FindPurchaseSuggestionResponse suggestionResponse;
    private Throwable thrownException;
    private UUID userId;
    private UUID studentId;
    private NotificacionEntity notification;

    @BeforeEach
    void setUp() {
        findPurchaseSuggestionForTomorrow = new FindPurchaseSuggestionForTomorrow(
                suggestionAiRepository,
                usuarioRepository,
                alumnoRepository,
                CLOCK
        );
    }

    @Test
    void givenTomorrowSuggestion_whenFindingSuggestion_thenReturnsSuggestion() {
        givenAuthenticatedStudent();
        givenTomorrowSuggestionExists();

        whenFindPurchaseSuggestionForTomorrow();

        thenSuggestionWasReturned();
        thenTomorrowSuggestionWasRequested();
    }

    @Test
    void givenNoTomorrowSuggestion_whenFindingSuggestion_thenReturnsNull() {
        givenAuthenticatedStudent();
        givenTomorrowSuggestionDoesNotExist();

        whenFindPurchaseSuggestionForTomorrow();

        thenSuggestionWasNotFound();
        thenTomorrowSuggestionWasRequested();
    }

    @Test
    void givenMissingAuthenticatedUser_whenFindingSuggestion_thenThrowsException() {
        givenSuggestionRequest();
        givenAuthenticatedUserDoesNotExist();

        whenFindPurchaseSuggestionForTomorrowFails(RuntimeException.class);

        thenErrorMessageIs("User not found");
    }

    @Test
    void givenMissingStudentForUser_whenFindingSuggestion_thenThrowsException() {
        givenSuggestionRequest();
        givenAuthenticatedUserExists();
        givenStudentDoesNotExistForUser();

        whenFindPurchaseSuggestionForTomorrowFails(RuntimeException.class);

        thenErrorMessageIs("Student not found for user");
    }

    private void givenAuthenticatedStudent() {
        givenSuggestionRequest();
        givenAuthenticatedUserExists();
        givenStudentExistsForUser();
    }

    private void givenSuggestionRequest() {
        suggestionRequest = new FindPurchaseSuggestionRequest(COGNITO_SUBJECT);
    }

    private void givenAuthenticatedUserExists() {
        userId = randomUUID();
        Usuario user = new Usuario();
        user.setId(userId);
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(user));
    }

    private void givenAuthenticatedUserDoesNotExist() {
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());
    }

    private void givenStudentExistsForUser() {
        studentId = randomUUID();
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
    }

    private void givenStudentDoesNotExistForUser() {
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.empty());
    }

    private void givenTomorrowSuggestionExists() {
        notification = notification();
        when(suggestionAiRepository.findByTipoAndStudentId(TOMORROW_DAY, studentId))
                .thenReturn(notification);
    }

    private void givenTomorrowSuggestionDoesNotExist() {
        when(suggestionAiRepository.findByTipoAndStudentId(TOMORROW_DAY, studentId))
                .thenReturn(null);
    }

    private void whenFindPurchaseSuggestionForTomorrow() {
        suggestionResponse = findPurchaseSuggestionForTomorrow.execute(suggestionRequest);
    }

    private <T extends Throwable> void whenFindPurchaseSuggestionForTomorrowFails(
            Class<T> expectedException) {
        Executable execution = () -> findPurchaseSuggestionForTomorrow.execute(
                suggestionRequest);
        thrownException = assertThrows(expectedException, execution);
    }

    private void thenSuggestionWasReturned() {
        assertNotNull(suggestionResponse);
        assertEquals(notification.getId(), suggestionResponse.id());
        assertEquals(notification.getTitulo(), suggestionResponse.titulo());
        assertEquals(notification.getTipo(), suggestionResponse.tipo());
    }

    private void thenSuggestionWasNotFound() {
        assertNull(suggestionResponse);
    }

    private void thenTomorrowSuggestionWasRequested() {
        verify(suggestionAiRepository).findByTipoAndStudentId(TOMORROW_DAY, studentId);
    }

    private void thenErrorMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private NotificacionEntity notification() {
        NotificacionEntity notification = new NotificacionEntity();
        notification.setId(randomUUID());
        notification.setTipo(TipoNotificacion.SALDO_BAJO);
        notification.setTitulo("Test");
        notification.setMensaje("Message");
        notification.setLeida(false);
        notification.setCreadoEn(LocalDateTime.of(2026, 7, 6, 10, 0));
        return notification;
    }
}
