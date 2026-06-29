package com.recred.backend.application;

import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.SuggestionAiRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import java.time.LocalDate;
import java.util.UUID;

public class FindPurchaseSuggestionForToday {

    private final SuggestionAiRepository suggestionAiRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlumnoRepository alumnoRepository;

    public FindPurchaseSuggestionForToday(
            SuggestionAiRepository suggestionAiRepository,
            UsuarioRepository usuarioRepository,
            AlumnoRepository alumnoRepository) {
        this.suggestionAiRepository = suggestionAiRepository;
        this.usuarioRepository = usuarioRepository;
        this.alumnoRepository = alumnoRepository;
    }

    public FindPurchaseSuggestionResponse execute(FindPurchaseSuggestionRequest request) {
        UUID studentId = getStudentId(request.cognitoSubject());
        String today = LocalDate.now().getDayOfWeek().name();
        NotificacionEntity entity = suggestionAiRepository.findByTipoAndStudentId(today, studentId);
        return mapToResponse(entity);
    }

    private UUID getStudentId(String cognitoSubject) {
        Usuario user = usuarioRepository.findByIdCognito(cognitoSubject)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return alumnoRepository.findStudentIdByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Student not found for user"));
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
