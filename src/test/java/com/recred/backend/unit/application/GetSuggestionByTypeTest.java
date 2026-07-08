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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSuggestionByTypeTest {

    private static final String COGNITO_SUBJECT = "cognito-id-123";
    private static final TipoSugerenciaIA SUGGESTION_TYPE = TipoSugerenciaIA.AGREGAR_PRODUCTO;

    @Mock
    private SuggestionAiRepository suggestionAiRepository;

    @Mock
    private KiosqueroRepository kiosqueroRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private GetSuggestionByType useCase;
    private UUID userId;
    private UUID buffetId;
    private UUID suggestionId;

    @BeforeEach
    void setUp() {
        useCase = new GetSuggestionByType(
                suggestionAiRepository,
                kiosqueroRepository,
                usuarioRepository
        );
    }

    @Test
    void givenAuthenticatedKiosqueroHasBuffet_whenGettingSuggestions_thenReturnsSuggestions() {
        givenAuthenticatedUserExists();
        givenUserHasBuffet();
        givenBuffetHasSuggestions();

        List<GetSuggestionResponse> response = whenGetSuggestionsByType();

        thenSuggestionsWereReturned(response);
    }

    @Test
    void givenAuthenticatedKiosqueroHasNoBuffet_whenGettingSuggestions_thenReturnsEmptyList() {
        givenAuthenticatedUserExists();
        givenUserHasNoBuffet();

        List<GetSuggestionResponse> response = whenGetSuggestionsByType();

        thenSuggestionsWereEmpty(response);
        thenSuggestionsWereNotRequested();
    }

    @Test
    void givenAuthenticatedUserDoesNotExist_whenGettingSuggestions_thenThrowsException() {
        givenAuthenticatedUserDoesNotExist();

        IllegalArgumentException exception = whenGetSuggestionsByTypeFails();

        thenUserNotFoundWasThrown(exception);
        thenBuffetAndSuggestionsWereNotRequested();
    }

    private void givenAuthenticatedUserExists() {
        userId = randomUUID();
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT))
                .thenReturn(Optional.of(user(userId)));
    }

    private void givenAuthenticatedUserDoesNotExist() {
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());
    }

    private void givenUserHasBuffet() {
        buffetId = randomUUID();
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(buffetId);
    }

    private void givenUserHasNoBuffet() {
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(null);
    }

    private void givenBuffetHasSuggestions() {
        suggestionId = randomUUID();
        when(suggestionAiRepository.findTop3ByBuffetIdAndTipo(buffetId, SUGGESTION_TYPE))
                .thenReturn(List.of(SugerenciaIA.builder().id(suggestionId).build()));
    }

    private List<GetSuggestionResponse> whenGetSuggestionsByType() {
        return useCase.execute(new GetSuggestionByTypeRequest(
                COGNITO_SUBJECT,
                SUGGESTION_TYPE
        ));
    }

    private IllegalArgumentException whenGetSuggestionsByTypeFails() {
        return assertThrows(IllegalArgumentException.class, this::whenGetSuggestionsByType);
    }

    private void thenSuggestionsWereReturned(List<GetSuggestionResponse> response) {
        assertEquals(1, response.size());
        assertEquals(suggestionId, response.get(0).id());
    }

    private void thenSuggestionsWereEmpty(List<GetSuggestionResponse> response) {
        assertTrue(response.isEmpty());
    }

    private void thenUserNotFoundWasThrown(IllegalArgumentException exception) {
        assertEquals("Usuario no encontrado", exception.getMessage());
    }

    private void thenSuggestionsWereNotRequested() {
        verifyNoInteractions(suggestionAiRepository);
    }

    private void thenBuffetAndSuggestionsWereNotRequested() {
        verifyNoInteractions(kiosqueroRepository, suggestionAiRepository);
    }

    private Usuario user(UUID requestedUserId) {
        Usuario user = new Usuario();
        user.setId(requestedUserId);
        return user;
    }
}
