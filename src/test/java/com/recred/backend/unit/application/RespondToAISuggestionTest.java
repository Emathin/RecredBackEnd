package com.recred.backend.unit.application;

import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.application.RespondToAISuggestionRequest;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RespondToAISuggestionTest {

    @Mock
    private SugerenciaIARepository sugerenciaIARepository;

    private RespondToAISuggestion useCase;

    @BeforeEach
    void setUp() {
        useCase = new RespondToAISuggestion(sugerenciaIARepository);
    }

    @Test
    void execute_WhenSuggestionExists_ShouldMarkItAsResponded() {
        UUID suggestionId = randomUUID();
        SugerenciaIAEntity suggestion = suggestion(suggestionId);
        givenSuggestionExists(suggestionId, suggestion);

        useCase.execute(request(suggestionId, true));

        assertSuggestionWasMarkedAsResponded(suggestion, true);
        verify(sugerenciaIARepository).guardar(suggestion);
    }

    @Test
    void execute_WhenSuggestionDoesNotExist_ShouldDoNothing() {
        UUID suggestionId = randomUUID();
        givenSuggestionDoesNotExist(suggestionId);

        useCase.execute(request(suggestionId, false));

        verify(sugerenciaIARepository, never()).guardar(any(SugerenciaIAEntity.class));
    }

    private RespondToAISuggestionRequest request(UUID suggestionId, boolean accepted) {
        return new RespondToAISuggestionRequest(suggestionId, accepted);
    }

    private void givenSuggestionExists(UUID suggestionId, SugerenciaIAEntity suggestion) {
        when(sugerenciaIARepository.findById(suggestionId)).thenReturn(Optional.of(suggestion));
    }

    private void givenSuggestionDoesNotExist(UUID suggestionId) {
        when(sugerenciaIARepository.findById(suggestionId)).thenReturn(Optional.empty());
    }

    private SugerenciaIAEntity suggestion(UUID suggestionId) {
        SugerenciaIAEntity suggestion = new SugerenciaIAEntity();
        suggestion.setId(suggestionId);
        return suggestion;
    }

    private void assertSuggestionWasMarkedAsResponded(
            SugerenciaIAEntity suggestion,
            boolean accepted) {
        assertEquals(accepted, suggestion.getAceptada());
        assertNotNull(suggestion.getRespondidaEn());
    }
}
