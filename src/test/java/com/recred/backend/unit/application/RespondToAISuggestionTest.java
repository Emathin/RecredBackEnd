package com.recred.backend.unit.application;

import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.application.RespondToAISuggestionRequest;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RespondToAISuggestionTest {

    private static final UUID SUGGESTION_ID = UUID.randomUUID();

    @Mock
    private SugerenciaIARepository sugerenciaIARepository;

    private RespondToAISuggestion useCase;

    @BeforeEach
    void setUp() {
        useCase = new RespondToAISuggestion(sugerenciaIARepository);
    }

    @Test
    void execute_WhenSuggestionExists_ShouldMarkItAsResponded() {
        SugerenciaIA suggestion = givenSuggestion();
        givenSuggestionExists(suggestion);
        RespondToAISuggestionRequest request = givenRequest(true);

        whenExecute(request);

        thenSuggestionIsRespondedAndSaved(suggestion, true);
    }

    @Test
    void execute_WhenSuggestionDoesNotExist_ShouldDoNothing() {
        givenSuggestionDoesNotExist();
        RespondToAISuggestionRequest request = givenRequest(false);

        whenExecute(request);

        thenNoSuggestionIsSaved();
    }

    private SugerenciaIA givenSuggestion() {
        SugerenciaIA suggestion = new SugerenciaIA();
        suggestion.setId(SUGGESTION_ID);
        return suggestion;
    }

    private RespondToAISuggestionRequest givenRequest(boolean accepted) {
        return new RespondToAISuggestionRequest(SUGGESTION_ID, accepted);
    }

    private void givenSuggestionExists(SugerenciaIA suggestion) {
        when(sugerenciaIARepository.findSugerenciaById(SUGGESTION_ID))
                .thenReturn(Optional.of(suggestion));
    }

    private void givenSuggestionDoesNotExist() {
        when(sugerenciaIARepository.findSugerenciaById(SUGGESTION_ID))
                .thenReturn(Optional.empty());
    }

    private void whenExecute(RespondToAISuggestionRequest request) {
        useCase.execute(request);
    }

    private void thenSuggestionIsRespondedAndSaved(SugerenciaIA suggestion, boolean expectedAccepted) {
        assertEquals(expectedAccepted, suggestion.getAceptada());
        assertNotNull(suggestion.getRespondidaEn());
        verify(sugerenciaIARepository).save(suggestion);
    }

    private void thenNoSuggestionIsSaved() {
        verify(sugerenciaIARepository, never()).save(any(SugerenciaIA.class));
    }
}
