package com.recred.backend.application;

import com.recred.backend.domain.repositories.AISuggestionRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public class RespondToAISuggestion {

    private final AISuggestionRepository aiSuggestionRepository;

    public RespondToAISuggestion(AISuggestionRepository aiSuggestionRepository) {
        this.aiSuggestionRepository = aiSuggestionRepository;
    }

    @Transactional
    public void execute(UUID sugerenciaId, boolean aceptada) {
        aiSuggestionRepository.findById(sugerenciaId).ifPresent(sugerencia -> {
            sugerencia.setAccepted(aceptada);
            sugerencia.setAnsweredAt(LocalDateTime.now());
            aiSuggestionRepository.save(sugerencia);
        });
    }
}
