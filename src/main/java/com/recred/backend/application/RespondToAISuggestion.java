package com.recred.backend.application;

import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public class RespondToAISuggestion {

    private final SugerenciaIARepository sugerenciaIARepository;

    public RespondToAISuggestion(SugerenciaIARepository sugerenciaIARepository) {
        this.sugerenciaIARepository = sugerenciaIARepository;
    }

    @Transactional
    public void execute(RespondToAISuggestionRequest request) {
        request.validate();

        findSuggestion(request)
                .ifPresent(suggestion -> respondToSuggestion(suggestion, request.accepted()));
    }

    private Optional<SugerenciaIAEntity> findSuggestion(RespondToAISuggestionRequest request) {
        return sugerenciaIARepository.findById(request.suggestionId());
    }

    private void respondToSuggestion(SugerenciaIAEntity suggestion, boolean accepted) {
        suggestion.setAceptada(accepted);
        suggestion.setRespondidaEn(LocalDateTime.now());
        sugerenciaIARepository.guardar(suggestion);
    }
}
