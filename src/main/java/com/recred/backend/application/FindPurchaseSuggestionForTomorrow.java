package com.recred.backend.application;

import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.SuggestionAiRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

public class FindPurchaseSuggestionForTomorrow {

    private final SuggestionAiRepository suggestionAiRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;
    private final Clock clock;

    public FindPurchaseSuggestionForTomorrow(
            SuggestionAiRepository suggestionAiRepository,
            UsuarioRepository usuarioRepository,
            AlumnoRepository alumnoRepository,
            Clock clock) {
        this.suggestionAiRepository = suggestionAiRepository;
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
        this.clock = clock;
    }

    public FindPurchaseSuggestionResponse execute(FindPurchaseSuggestionRequest suggestionRequest) {
        UUID studentId = findStudentIdForAuthenticatedUser(suggestionRequest.cognitoSubject());
        NotificacionEntity suggestion = findTomorrowSuggestion(studentId);
        return mapToResponse(suggestion);
    }

    private UUID findStudentIdForAuthenticatedUser(String cognitoSubject) {
        Usuario user = usuarioRepository.findByIdCognito(cognitoSubject)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return alumnoRepository.findStudentIdByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Student not found for user"));
    }

    private NotificacionEntity findTomorrowSuggestion(UUID studentId) {
        return suggestionAiRepository.findByTipoAndStudentId(tomorrowDayName(), studentId);
    }

    private String tomorrowDayName() {
        return LocalDate.now(clock).plusDays(1).getDayOfWeek().name();
    }

    private FindPurchaseSuggestionResponse mapToResponse(NotificacionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new FindPurchaseSuggestionResponse(
                entity.getId(),
                entity.getTipo(),
                entity.getTitulo(),
                entity.getMensaje(),
                entity.getLeida(),
                entity.getCreadoEn(),
                entity.getLeidaEn()
        );
    }
}
