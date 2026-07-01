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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindPurchaseSuggestionForTodayTest {

    @Mock
    private SuggestionAiRepository suggestionAiRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AlumnoRepository alumnoRepository;

    private FindPurchaseSuggestionForToday useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindPurchaseSuggestionForToday(
                suggestionAiRepository, usuarioRepository, alumnoRepository);
    }

    @Test
    void execute_ShouldReturnSuggestionForToday() {
        String cognitoSubject = "test-sub";
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();

        Usuario usuario = new Usuario();
        usuario.setId(userId);

        NotificacionEntity notification = new NotificacionEntity();
        notification.setId(notifId);
        notification.setTipo(TipoNotificacion.SALDO_BAJO);
        notification.setTitulo("Test");
        notification.setMensaje("Message");
        notification.setLeida(false);
        notification.setCreadoEn(LocalDateTime.now());

        String today = LocalDate.now().getDayOfWeek().name();

        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(usuario));
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
        when(suggestionAiRepository.findByTipoAndStudentId(today, studentId)).thenReturn(notification);

        FindPurchaseSuggestionRequest request = new FindPurchaseSuggestionRequest(cognitoSubject);
        FindPurchaseSuggestionResponse response = useCase.execute(request);

        assertNotNull(response);
        assertEquals(notifId, response.id());
        assertEquals("Test", response.titulo());
        assertEquals(TipoNotificacion.SALDO_BAJO, response.tipo());
    }

    @Test
    void execute_ShouldReturnNull_WhenNoSuggestionFound() {
        String cognitoSubject = "test-sub";
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        Usuario usuario = new Usuario();
        usuario.setId(userId);

        String today = LocalDate.now().getDayOfWeek().name();

        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(usuario));
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
        when(suggestionAiRepository.findByTipoAndStudentId(today, studentId)).thenReturn(null);

        FindPurchaseSuggestionRequest request = new FindPurchaseSuggestionRequest(cognitoSubject);
        FindPurchaseSuggestionResponse response = useCase.execute(request);

        assertNull(response);
    }
}
