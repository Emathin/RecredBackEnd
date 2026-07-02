package com.recred.backend.application;

import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.SuggestionAiRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GetSuggestionByType {

    private final SuggestionAiRepository suggestionAiRepository;
    private final KiosqueroRepository kiosqueroRepository;
    private final UsuarioRepository usuarioRepository;

    public GetSuggestionByType(SuggestionAiRepository suggestionAiRepository,
                               KiosqueroRepository kiosqueroRepository,
                               UsuarioRepository usuarioRepository) {
        this.suggestionAiRepository = suggestionAiRepository;
        this.kiosqueroRepository = kiosqueroRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<GetSuggestionResponse> execute(GetSuggestionByTypeRequest request) {
        UUID usuarioId = getUsuarioId(request.cognitoSubject());
        UUID buffetId = kiosqueroRepository.findBuffetByUsuarioId(usuarioId);
        if (buffetId == null) {
            return List.of();
        }
        return suggestionAiRepository.findTop3ByBuffetIdAndTipo(buffetId, request.tipo())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private UUID getUsuarioId(String cognitoSubject) {
        return usuarioRepository.findByIdCognito(cognitoSubject)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"))
                .getId();
    }

    private GetSuggestionResponse mapToResponse(SugerenciaIA suggestion) {
        return new GetSuggestionResponse(
                suggestion.getId(),
                suggestion.getAlumnoId(),
                suggestion.getBuffetId(),
                suggestion.getProductoId(),
                suggestion.getTitulo(),
                suggestion.getMensaje(),
                suggestion.getMetadata()
        );
    }
}
