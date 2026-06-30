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
import com.recred.backend.application.GetStudentPreferencesByUserRequest;
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
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
        when(detectStudentPreferences.execute(
                new DetectStudentPreferencesRequest(studentId, buffetId)))
                .thenReturn(Optional.of(detected));

        ResponseEntity<StudentPreferenceResponseDTO> response = controller.detect(userId, buffetId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(detected.titulo(), response.getBody().titulo());
    }

    @Test
    void returnNoContentWhenNoAIPreferenceIsDetected() {
        UUID userId = randomUUID();
        UUID studentId = randomUUID();
        UUID buffetId = randomUUID();
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
        when(detectStudentPreferences.execute(
                new DetectStudentPreferencesRequest(studentId, buffetId)))
                .thenReturn(Optional.empty());

        ResponseEntity<StudentPreferenceResponseDTO> response = controller.detect(userId, buffetId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void detectPreferenceUsingUserId() {
        UUID userId = randomUUID();
        UUID studentId = randomUUID();
        UUID buffetId = randomUUID();
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
        DetectStudentPreferencesRequest request =
                new DetectStudentPreferencesRequest(studentId, buffetId);
        when(detectStudentPreferences.execute(request)).thenReturn(Optional.empty());

        ResponseEntity<StudentPreferenceResponseDTO> response = controller.detect(userId, buffetId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(detectStudentPreferences).execute(request);
    }

    @Test
    void returnNoContentWhenPreferenceListIsEmpty() {
        UUID userId = randomUUID();
        when(getStudentPreferences.executeByUserId(
                new GetStudentPreferencesByUserRequest(userId, false, null)))
                .thenReturn(new GetStudentPreferencesResponse(List.of()));

        ResponseEntity<List<StudentPreferenceResponseDTO>> response =
                controller.getPreferences(userId, false, null);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void returnOkWithPreferencesListWhenTheyExist() {
        UUID userId = randomUUID();
        when(getStudentPreferences.executeByUserId(
                new GetStudentPreferencesByUserRequest(userId, false, null)))
                .thenReturn(new GetStudentPreferencesResponse(List.of(preference(
                        randomUUID(),
                        null,
                        null,
                        false,
                        null,
                        null,
                        "Test",
                        "Msg"
                ))));

        ResponseEntity<List<StudentPreferenceResponseDTO>> response =
                controller.getPreferences(userId, false, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void returnOkWithParentPreferenceView() {
        UUID userId = randomUUID();
        UUID childId = randomUUID();
        UUID childUserId = randomUUID();
        when(getStudentPreferences.executeByUserId(
                new GetStudentPreferencesByUserRequest(userId, false, null)))
                .thenReturn(new GetStudentPreferencesResponse(List.of(preference(
                        randomUUID(),
                        childId,
                        childUserId,
                        true,
                        "Ana",
                        "Lopez",
                        "Probar fruta",
                        "Puede gustarle"
                ))));

        ResponseEntity<List<StudentPreferenceResponseDTO>> response =
                controller.getPreferences(userId, false, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Ana Lopez: Probar fruta", response.getBody().get(0).titulo());
        assertEquals(childId, response.getBody().get(0).alumnoId());
        assertEquals(childUserId, response.getBody().get(0).alumnoUserId());
    }

    @Test
    void respondToAISuggestionSuccessfully() {
        UUID sugerenciaId = randomUUID();

        ResponseEntity<Void> response = controller.respond(sugerenciaId, true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(respondToAISuggestion).execute(
                new RespondToAISuggestionRequest(sugerenciaId, true));
    }

    @Test
    void agregarFavoritoSuccessfully() {
        UUID studentId = randomUUID();
        UUID productoId = randomUUID();

        ResponseEntity<Void> response = controller.agregarFavorito(studentId, productoId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(addFavoriteProduct).execute(new AddFavoriteProductRequest(studentId, productoId));
    }

    @Test
    void removerFavoritoSuccessfully() {
        UUID studentId = randomUUID();
        UUID productoId = randomUUID();

        ResponseEntity<Void> response = controller.removerFavorito(studentId, productoId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(deleteFavoriteProduct).execute(new DeleteFavoriteProductRequest(studentId, productoId));
    }

    @Test
    void obtenerFavoritosSuccessfully() {
        UUID studentId = randomUUID();
        UUID productId = randomUUID();
        GetFavoritesByStudentResponse responseDTO = new GetFavoritesByStudentResponse(
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

        when(getFavoritesByStudent.execute(new GetFavoritesByStudentRequest(studentId))).thenReturn(Set.of(responseDTO));

        ResponseEntity<List<ProductResponseDTO>> response = controller.obtenerFavoritos(studentId);

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
}

