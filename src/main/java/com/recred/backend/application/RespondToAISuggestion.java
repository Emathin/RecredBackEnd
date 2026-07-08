package com.recred.backend.application;

import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public class RespondToAISuggestion {

    private final SugerenciaIARepository sugerenciaIARepository;

    public RespondToAISuggestion(SugerenciaIARepository sugerenciaIARepository) {
        this.sugerenciaIARepository = sugerenciaIARepository;
    }

    @Transactional
    public RespondToAISuggestionResponse execute(RespondToAISuggestionRequest request) {
        request.validate();

        findSuggestion(request)
                .ifPresent(suggestion -> respondToSuggestion(suggestion, request.accepted()));
        
        return new RespondToAISuggestionResponse();
    }

    private Optional<SugerenciaIA> findSuggestion(RespondToAISuggestionRequest request) {
        return sugerenciaIARepository.findSugerenciaById(request.suggestionId());
    }

    private void respondToSuggestion(SugerenciaIA suggestion, boolean accepted) {
        suggestion.setAceptada(accepted);
        suggestion.setRespondidaEn(LocalDateTime.now());
        saveSuggestion(suggestion);
    }
    
    private void saveSuggestion(SugerenciaIA suggestion) {
        sugerenciaIARepository.save(suggestion);
    }
}
