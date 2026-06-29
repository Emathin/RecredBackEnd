package com.recred.backend.unit.application;

import com.recred.backend.application.GetSuggestionByType;
import com.recred.backend.application.GetSuggestionByTypeRequest;
import com.recred.backend.application.GetSuggestionResponse;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.SuggestionAiRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetSuggestionByTypeTest {

    private SuggestionAiRepository suggestionAiRepository;
    private KiosqueroRepository kiosqueroRepository;
    private UsuarioRepository usuarioRepository;
    private GetSuggestionByType useCase;

    @BeforeEach
    void setUp() {
        suggestionAiRepository = mock(SuggestionAiRepository.class);
        kiosqueroRepository = mock(KiosqueroRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        useCase = new GetSuggestionByType(suggestionAiRepository, kiosqueroRepository, usuarioRepository);
    }

    @Test
    void executeReturnsSuggestionsWhenBuffetExists() {
        String cognitoSubject = "cognito-id-123";
        UUID usuarioId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        TipoSugerenciaIA tipo = TipoSugerenciaIA.AGREGAR_PRODUCTO;
        SugerenciaIA suggestion = SugerenciaIA.builder().id(UUID.randomUUID()).build();
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(usuario));
        when(kiosqueroRepository.findBuffetByUsuarioId(usuarioId)).thenReturn(buffetId);
        when(suggestionAiRepository.findTop3ByBuffetIdAndTipo(buffetId, tipo)).thenReturn(List.of(suggestion));

        GetSuggestionByTypeRequest request = new GetSuggestionByTypeRequest(cognitoSubject, tipo);
        List<GetSuggestionResponse> result = useCase.execute(request);

        assertEquals(1, result.size());
        assertEquals(suggestion.getId(), result.get(0).id());
    }

    @Test
    void executeReturnsEmptyListWhenBuffetDoesNotExist() {
        String cognitoSubject = "cognito-id-123";
        UUID usuarioId = UUID.randomUUID();
        TipoSugerenciaIA tipo = TipoSugerenciaIA.AGREGAR_PRODUCTO;
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(usuario));
        when(kiosqueroRepository.findBuffetByUsuarioId(usuarioId)).thenReturn(null);

        GetSuggestionByTypeRequest request = new GetSuggestionByTypeRequest(cognitoSubject, tipo);
        List<GetSuggestionResponse> result = useCase.execute(request);

        assertTrue(result.isEmpty());
    }
}
