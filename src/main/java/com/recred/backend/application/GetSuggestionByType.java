package com.recred.backend.application;

import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.SuggestionAiRepository;

import java.util.List;
import java.util.UUID;

public class GetSuggestionByType {

    private final SuggestionAiRepository suggestionAiRepository;
    private final KiosqueroRepository kiosqueroRepository;

    public GetSuggestionByType(SuggestionAiRepository suggestionAiRepository,
                               KiosqueroRepository kiosqueroRepository) {
        this.suggestionAiRepository = suggestionAiRepository;
        this.kiosqueroRepository = kiosqueroRepository;
    }

    public List<SugerenciaIA> execute(UUID usuarioId, TipoSugerenciaIA tipo) {
        UUID buffetId = kiosqueroRepository.findBuffetByUsuarioId(usuarioId);
        if (buffetId == null) {
            return List.of();
        }
        return suggestionAiRepository.findTop3ByBuffetIdAndTipo(buffetId, tipo);
    }
}
