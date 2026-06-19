package com.recred.backend.application;

import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        UUID suggestionId = UUID.randomUUID();
        SugerenciaIAEntity suggestion = new SugerenciaIAEntity();
        suggestion.setId(suggestionId);
        when(sugerenciaIARepository.findById(suggestionId)).thenReturn(Optional.of(suggestion));

        useCase.execute(suggestionId, true);

        assertEquals(Boolean.TRUE, suggestion.getAceptada());
        assertNotNull(suggestion.getRespondidaEn());
        verify(sugerenciaIARepository).guardar(suggestion);
    }

    @Test
    void execute_WhenSuggestionDoesNotExist_ShouldDoNothing() {
        UUID suggestionId = UUID.randomUUID();
        when(sugerenciaIARepository.findById(suggestionId)).thenReturn(Optional.empty());

        useCase.execute(suggestionId, false);

        verify(sugerenciaIARepository, never()).guardar(
                org.mockito.ArgumentMatchers.any(SugerenciaIAEntity.class)
        );
    }
}
