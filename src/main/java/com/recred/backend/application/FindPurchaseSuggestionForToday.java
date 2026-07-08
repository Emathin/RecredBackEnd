package com.recred.backend.application;

import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.SuggestionAiRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

public class FindPurchaseSuggestionForToday {

    private final SuggestionAiRepository suggestionAiRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final Clock clock;

    public FindPurchaseSuggestionForToday(
            SuggestionAiRepository suggestionAiRepository,
            UsuarioRepository usuarioRepository,
            AlumnoRepository alumnoRepository) {
        this(suggestionAiRepository, usuarioRepository, alumnoRepository,
                Clock.systemDefaultZone());
    }

    public FindPurchaseSuggestionForToday(
            SuggestionAiRepository suggestionAiRepository,
            UsuarioRepository usuarioRepository,
            AlumnoRepository alumnoRepository,
            Clock clock) {
        this.suggestionAiRepository = suggestionAiRepository;
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.clock = clock;
    }

    public FindPurchaseSuggestionResponse execute(FindPurchaseSuggestionRequest request) {
        UUID studentId = findStudentIdForAuthenticatedUser(request.cognitoSubject());
        NotificacionEntity suggestion = findSuggestionForToday(studentId);
        return responseFrom(suggestion);
    }

    private UUID findStudentIdForAuthenticatedUser(String cognitoSubject) {
        Usuario user = findAuthenticatedUser(cognitoSubject);
        return alumnoRepository.findStudentIdByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Student not found for user"));
    }

    private Usuario findAuthenticatedUser(String cognitoSubject) {
        return usuarioRepository.findByIdCognito(cognitoSubject)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private NotificacionEntity findSuggestionForToday(UUID studentId) {
        return suggestionAiRepository.findByTipoAndStudentId(todayNotificationType(), studentId);
    }

    private String todayNotificationType() {
        return LocalDate.now(clock).getDayOfWeek().name();
    }

    private FindPurchaseSuggestionResponse responseFrom(NotificacionEntity suggestion) {
        if (suggestion == null) {
            return null;
        }
        return new FindPurchaseSuggestionResponse(
                suggestion.getId(),
                suggestion.getTipo(),
                suggestion.getTitulo(),
                suggestion.getMensaje(),
                suggestion.getLeida(),
                suggestion.getCreadoEn(),
                suggestion.getLeidaEn()
        );
    }
}
