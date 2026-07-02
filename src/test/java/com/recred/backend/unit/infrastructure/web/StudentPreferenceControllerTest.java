package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.AddFavoriteProduct;
import com.recred.backend.application.AddFavoriteProductRequest;
import com.recred.backend.application.DeleteFavoriteProduct;
import com.recred.backend.application.DeleteFavoriteProductRequest;
import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.DetectStudentPreferencesRequest;
import com.recred.backend.application.DetectStudentPreferencesResponse;
import com.recred.backend.application.GetFavoritesByStudent;
import com.recred.backend.application.GetFavoritesByStudentRequest;
import com.recred.backend.application.GetFavoritesByStudentResponse;
import com.recred.backend.application.GetStudentPreferences;
import com.recred.backend.application.GetStudentPreferencesRequest;
import com.recred.backend.application.GetStudentPreferencesResponse;
import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.application.RespondToAISuggestionRequest;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.infrastructure.web.ProductResponseDTO;
import com.recred.backend.infrastructure.web.StudentPreferenceController;
import com.recred.backend.infrastructure.web.StudentPreferenceResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentPreferenceControllerTest {

    private DetectStudentPreferences detectStudentPreferences;
    private GetStudentPreferences getStudentPreferences;
    private RespondToAISuggestion respondToAISuggestion;
    private AlumnoRepository alumnoRepository;
    private AddFavoriteProduct addFavoriteProduct;
    private DeleteFavoriteProduct deleteFavoriteProduct;
    private GetFavoritesByStudent getFavoritesByStudent;
    private StudentPreferenceController controller;

    @BeforeEach
    void setUp() {
        detectStudentPreferences = mock(DetectStudentPreferences.class);
        getStudentPreferences = mock(GetStudentPreferences.class);
        respondToAISuggestion = mock(RespondToAISuggestion.class);
        alumnoRepository = mock(AlumnoRepository.class);
        addFavoriteProduct = mock(AddFavoriteProduct.class);
        deleteFavoriteProduct = mock(DeleteFavoriteProduct.class);
        getFavoritesByStudent = mock(GetFavoritesByStudent.class);

        controller = new StudentPreferenceController(
                detectStudentPreferences,
                getStudentPreferences,
                respondToAISuggestion,
                alumnoRepository,
                addFavoriteProduct,
                deleteFavoriteProduct,
                getFavoritesByStudent
        );
    }

    @Test
    void returnPreferenceResponseWhenPreferenceIsDetected() {
        UUID userId = randomUUID();
        UUID studentId = randomUUID();
        UUID buffetId = randomUUID();
        UUID productId = randomUUID();
        DetectStudentPreferencesResponse detected = new DetectStudentPreferencesResponse(
                "Yogur con Cereales",
                "He notado que te gusta...",
                productId,
                "Frecuencia alta"
        );
        givenStudentIdForUser(userId, studentId);
        givenPreferenceDetected(studentId, buffetId, detected);

        ResponseEntity<StudentPreferenceResponseDTO> response =
                whenDetectingPreference(userId, buffetId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(detected.titulo(), response.getBody().titulo());
    }

    @Test
    void returnNoContentWhenNoAIPreferenceIsDetected() {
        UUID userId = randomUUID();
        UUID studentId = randomUUID();
        UUID buffetId = randomUUID();
        givenStudentIdForUser(userId, studentId);
        givenNoPreferenceDetected(studentId, buffetId);

        ResponseEntity<StudentPreferenceResponseDTO> response =
                whenDetectingPreference(userId, buffetId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void detectPreferenceUsingUserId() {
        UUID userId = randomUUID();
        UUID studentId = randomUUID();
        UUID buffetId = randomUUID();
        DetectStudentPreferencesRequest request =
                new DetectStudentPreferencesRequest(studentId, buffetId);
        givenStudentIdForUser(userId, studentId);
        givenNoPreferenceDetected(request);

        ResponseEntity<StudentPreferenceResponseDTO> response =
                whenDetectingPreference(userId, buffetId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(detectStudentPreferences).execute(request);
    }

    @Test
    void returnNoContentWhenPreferenceListIsEmpty() {
        UUID userId = randomUUID();
        givenPreferencesByUser(userId);

        ResponseEntity<List<StudentPreferenceResponseDTO>> response =
                whenGettingPreferences(userId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void returnOkWithPreferencesListWhenTheyExist() {
        UUID userId = randomUUID();
        givenPreferencesByUser(userId, preference(
                randomUUID(),
                null,
                null,
                false,
                null,
                null,
                "Test",
                "Msg"
        ));

        ResponseEntity<List<StudentPreferenceResponseDTO>> response =
                whenGettingPreferences(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void returnOkWithParentPreferenceView() {
        UUID userId = randomUUID();
        UUID childId = randomUUID();
        UUID childUserId = randomUUID();
        givenPreferencesByUser(userId, preference(
                randomUUID(),
                childId,
                childUserId,
                true,
                "Ana",
                "Lopez",
                "Probar fruta",
                "Puede gustarle"
        ));

        ResponseEntity<List<StudentPreferenceResponseDTO>> response =
                whenGettingPreferences(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Ana Lopez: Probar fruta", response.getBody().get(0).titulo());
        assertEquals(childId, response.getBody().get(0).alumnoId());
        assertEquals(childUserId, response.getBody().get(0).alumnoUserId());
    }

    @Test
    void respondToAISuggestionSuccessfully() {
        UUID sugerenciaId = randomUUID();

        ResponseEntity<Void> response = whenRespondingToSuggestion(sugerenciaId, true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(respondToAISuggestion).execute(
                new RespondToAISuggestionRequest(sugerenciaId, true));
    }

    @Test
    void addFavoriteSuccessfully() {
        UUID studentId = randomUUID();
        UUID productoId = randomUUID();

        ResponseEntity<Void> response = whenAddingFavorite(studentId, productoId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(addFavoriteProduct).execute(new AddFavoriteProductRequest(studentId, productoId));
    }

    @Test
    void removeFavoriteSuccessfully() {
        UUID studentId = randomUUID();
        UUID productoId = randomUUID();

        ResponseEntity<Void> response = whenRemovingFavorite(studentId, productoId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(deleteFavoriteProduct).execute(
                new DeleteFavoriteProductRequest(studentId, productoId));
    }

    @Test
    void getFavoritesSuccessfully() {
        UUID studentId = randomUUID();
        UUID productId = randomUUID();
        givenFavoriteProducts(studentId, favoriteProduct(productId));

        ResponseEntity<List<ProductResponseDTO>> response = whenGettingFavorites(studentId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(productId, response.getBody().get(0).id());
        verify(getFavoritesByStudent).execute(new GetFavoritesByStudentRequest(studentId));
    }

    private GetStudentPreferencesResponse.Preference preference(
            UUID suggestionId,
            UUID studentId,
            UUID studentUserId,
            boolean parentView,
            String firstName,
            String lastName,
            String title,
            String message
    ) {
        return new GetStudentPreferencesResponse.Preference(
                suggestionId,
                studentId,
                studentUserId,
                firstName,
                lastName,
                TipoSugerenciaIA.PREFERENCIA_DETECTADA,
                title,
                message,
                null,
                null,
                parentView
        );
    }

    private void givenStudentIdForUser(UUID userId, UUID studentId) {
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
    }

    private void givenPreferenceDetected(
            UUID studentId,
            UUID buffetId,
            DetectStudentPreferencesResponse detected) {
        when(detectStudentPreferences.execute(
                new DetectStudentPreferencesRequest(studentId, buffetId)))
                .thenReturn(Optional.of(detected));
    }

    private void givenNoPreferenceDetected(UUID studentId, UUID buffetId) {
        givenNoPreferenceDetected(new DetectStudentPreferencesRequest(studentId, buffetId));
    }

    private void givenNoPreferenceDetected(DetectStudentPreferencesRequest request) {
        when(detectStudentPreferences.execute(request)).thenReturn(Optional.empty());
    }

    private void givenPreferencesByUser(
            UUID userId,
            GetStudentPreferencesResponse.Preference... preferences) {
        when(getStudentPreferences.execute(userPreferencesRequest(userId)))
                .thenReturn(new GetStudentPreferencesResponse(List.of(preferences)));
    }

    private void givenFavoriteProducts(
            UUID studentId,
            GetFavoritesByStudentResponse... favorites) {
        when(getFavoritesByStudent.execute(new GetFavoritesByStudentRequest(studentId)))
                .thenReturn(Set.of(favorites));
    }

    private ResponseEntity<StudentPreferenceResponseDTO> whenDetectingPreference(
            UUID userId,
            UUID buffetId) {
        return controller.detect(userId, buffetId);
    }

    private ResponseEntity<List<StudentPreferenceResponseDTO>> whenGettingPreferences(
            UUID userId) {
        return controller.getPreferences(userId, false, null);
    }

    private ResponseEntity<Void> whenRespondingToSuggestion(
            UUID suggestionId,
            boolean accepted) {
        return controller.respond(suggestionId, accepted);
    }

    private ResponseEntity<Void> whenAddingFavorite(UUID studentId, UUID productId) {
        return controller.agregarFavorito(studentId, productId);
    }

    private ResponseEntity<Void> whenRemovingFavorite(UUID studentId, UUID productId) {
        return controller.removerFavorito(studentId, productId);
    }

    private ResponseEntity<List<ProductResponseDTO>> whenGettingFavorites(UUID studentId) {
        return controller.obtenerFavoritos(studentId);
    }

    private GetStudentPreferencesRequest userPreferencesRequest(UUID userId) {
        return GetStudentPreferencesRequest.byUser(userId, false, null);
    }

    private GetFavoritesByStudentResponse favoriteProduct(UUID productId) {
        return new GetFavoritesByStudentResponse(
                productId,
                "Test",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
