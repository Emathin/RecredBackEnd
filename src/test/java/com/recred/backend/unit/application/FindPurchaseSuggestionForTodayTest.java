package com.recred.backend.unit.application;

import com.recred.backend.application.FindPurchaseSuggestionForToday;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindPurchaseSuggestionForTodayTest {

    private static final ZoneId TEST_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 6);

    @Mock
    private SuggestionAiRepository suggestionAiRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AlumnoRepository alumnoRepository;

    private FindPurchaseSuggestionForToday useCase;

    private String cognitoSubject;
    private UUID userId;
    private UUID studentId;
    private UUID notificationId;
    private NotificacionEntity notification;
    private LocalDateTime createdAt;
    private FindPurchaseSuggestionResponse response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(TODAY.atStartOfDay(TEST_ZONE).toInstant(), TEST_ZONE);
        useCase = new FindPurchaseSuggestionForToday(
                suggestionAiRepository,
                usuarioRepository,
                alumnoRepository,
                fixedClock);
    }

    @Test
    void givenSuggestionForToday_whenFindingPurchaseSuggestion_thenReturnsSuggestion() {
        givenAuthenticatedStudent();
        givenAuthenticatedUserExists();
        givenStudentBelongsToUser();
        givenTodaySuggestionExists();

        whenFindingPurchaseSuggestion();

        thenTodaySuggestionWasRequested();
        thenSuggestionIsReturned();
    }

    @Test
    void givenNoSuggestionForToday_whenFindingPurchaseSuggestion_thenReturnsNull() {
        givenAuthenticatedStudent();
        givenAuthenticatedUserExists();
        givenStudentBelongsToUser();
        givenTodaySuggestionDoesNotExist();

        whenFindingPurchaseSuggestion();

        thenTodaySuggestionWasRequested();
        thenSuggestionIsNull();
    }

    @Test
    void givenMissingAuthenticatedUser_whenFindingPurchaseSuggestion_thenFails() {
        givenAuthenticatedStudent();
        givenAuthenticatedUserDoesNotExist();

        whenFindingPurchaseSuggestionFails();

        thenFindFailsWith("User not found");
    }

    @Test
    void givenAuthenticatedUserWithoutStudent_whenFindingPurchaseSuggestion_thenFails() {
        givenAuthenticatedStudent();
        givenAuthenticatedUserExists();
        givenUserHasNoStudent();

        whenFindingPurchaseSuggestionFails();

        thenFindFailsWith("Student not found for user");
    }

    private void givenAuthenticatedStudent() {
        cognitoSubject = "test-sub";
        userId = UUID.randomUUID();
        studentId = UUID.randomUUID();
    }

    private void givenAuthenticatedUserExists() {
        Usuario user = new Usuario();
        user.setId(userId);
        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(user));
    }

    private void givenAuthenticatedUserDoesNotExist() {
        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.empty());
    }

    private void givenStudentBelongsToUser() {
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
    }

    private void givenUserHasNoStudent() {
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.empty());
    }

    private void givenTodaySuggestionExists() {
        notificationId = UUID.randomUUID();
        createdAt = LocalDateTime.of(2026, 7, 6, 8, 30);
        notification = new NotificacionEntity();
        notification.setId(notificationId);
        notification.setTipo(TipoNotificacion.SALDO_BAJO);
        notification.setTitulo("Test");
        notification.setMensaje("Message");
        notification.setLeida(false);
        notification.setCreadoEn(createdAt);

        when(suggestionAiRepository.findByTipoAndStudentId(todayNotificationType(), studentId))
                .thenReturn(notification);
    }

    private void givenTodaySuggestionDoesNotExist() {
        when(suggestionAiRepository.findByTipoAndStudentId(todayNotificationType(), studentId))
                .thenReturn(null);
    }

    private void whenFindingPurchaseSuggestion() {
        response = useCase.execute(request());
    }

    private void whenFindingPurchaseSuggestionFails() {
        thrownException = assertThrows(RuntimeException.class, () -> useCase.execute(request()));
    }

    private void thenTodaySuggestionWasRequested() {
        verify(suggestionAiRepository).findByTipoAndStudentId(todayNotificationType(), studentId);
    }

    private void thenSuggestionIsReturned() {
        assertEquals(notificationId, response.id());
        assertEquals(TipoNotificacion.SALDO_BAJO, response.tipo());
        assertEquals("Test", response.titulo());
        assertEquals("Message", response.mensaje());
        assertEquals(false, response.leida());
        assertEquals(createdAt, response.creadoEn());
    }

    private void thenSuggestionIsNull() {
        assertNull(response);
    }

    private void thenFindFailsWith(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private FindPurchaseSuggestionRequest request() {
        return new FindPurchaseSuggestionRequest(cognitoSubject);
    }

    private String todayNotificationType() {
        return TODAY.getDayOfWeek().name();
    }
}
