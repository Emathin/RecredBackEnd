package com.recred.backend.unit.application;

import com.recred.backend.application.GetSuggestionByType;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.SuggestionAiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetSuggestionByTypeTest {

    private SuggestionAiRepository suggestionAiRepository;
    private KiosqueroRepository kiosqueroRepository;
    private GetSuggestionByType useCase;

    @BeforeEach
    void setUp() {
        suggestionAiRepository = mock(SuggestionAiRepository.class);
        kiosqueroRepository = mock(KiosqueroRepository.class);
        useCase = new GetSuggestionByType(suggestionAiRepository, kiosqueroRepository);
    }

    @Test
    void executeReturnsSuggestionsWhenBuffetExists() {
        UUID usuarioId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        TipoSugerenciaIA tipo = TipoSugerenciaIA.AGREGAR_PRODUCTO;
        SugerenciaIA suggestion = SugerenciaIA.builder().id(UUID.randomUUID()).build();

        when(kiosqueroRepository.findBuffetByUsuarioId(usuarioId)).thenReturn(buffetId);
        when(suggestionAiRepository.findTop3ByBuffetIdAndTipo(buffetId, tipo)).thenReturn(List.of(suggestion));

        List<SugerenciaIA> result = useCase.execute(usuarioId, tipo);

        assertEquals(1, result.size());
        assertEquals(suggestion.getId(), result.get(0).getId());
    }

    @Test
    void executeReturnsEmptyListWhenBuffetDoesNotExist() {
        UUID usuarioId = UUID.randomUUID();
        TipoSugerenciaIA tipo = TipoSugerenciaIA.AGREGAR_PRODUCTO;

        when(kiosqueroRepository.findBuffetByUsuarioId(usuarioId)).thenReturn(null);

        List<SugerenciaIA> result = useCase.execute(usuarioId, tipo);

        assertTrue(result.isEmpty());
    }
}
