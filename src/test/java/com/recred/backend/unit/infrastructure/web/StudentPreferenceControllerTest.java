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
import com.recred.backend.infrastructure.web.PlanAccessGuard;
import com.recred.backend.infrastructure.web.ProductResponseDTO;
import com.recred.backend.infrastructure.web.SchoolLicenseAccessGuard;
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
    private PlanAccessGuard planAccessGuard;
    private SchoolLicenseAccessGuard schoolLicenseAccessGuard;
    private StudentPreferenceController controller;

    private UUID userId;
    private UUID studentId;
    private UUID buffetId;
    private UUID productId;
    private UUID suggestionId;
    private UUID childId;
    private UUID childUserId;
    private DetectStudentPreferencesResponse detectedPreference;
    private ResponseEntity<StudentPreferenceResponseDTO> preferenceResponse;
    private ResponseEntity<List<StudentPreferenceResponseDTO>> preferencesResponse;
    private ResponseEntity<List<ProductResponseDTO>> favoriteProductsResponse;
    private ResponseEntity<Void> voidResponse;

    @BeforeEach
    void setUp() {
        detectStudentPreferences = mock(DetectStudentPreferences.class);
        getStudentPreferences = mock(GetStudentPreferences.class);
        respondToAISuggestion = mock(RespondToAISuggestion.class);
        alumnoRepository = mock(AlumnoRepository.class);
        addFavoriteProduct = mock(AddFavoriteProduct.class);
        deleteFavoriteProduct = mock(DeleteFavoriteProduct.class);
        getFavoritesByStudent = mock(GetFavoritesByStudent.class);
        planAccessGuard = mock(PlanAccessGuard.class);
        schoolLicenseAccessGuard = mock(SchoolLicenseAccessGuard.class);

        controller = new StudentPreferenceController(
                detectStudentPreferences,
                getStudentPreferences,
                respondToAISuggestion,
                alumnoRepository,
                addFavoriteProduct,
                deleteFavoriteProduct,
                getFavoritesByStudent,
                planAccessGuard,
                schoolLicenseAccessGuard
        );
    }

    @Test
    void givenDetectedPreference_whenDetectingPreference_thenReturnsPreferenceResponse() {
        givenDetectedPreferenceScenario();
        givenStudentIdForUser();
        givenPreferenceDetected();

        whenDetectingPreference();

        thenPreferenceResponseIsReturned();
        thenIntermediatePlanWasRequired();
        thenStudentSchoolLicenseWasRequired();
    }

    @Test
    void givenNoPreferenceDetected_whenDetectingPreference_thenReturnsNoContent() {
        givenNoPreferenceDetectedScenario();
        givenStudentIdForUser();
        givenNoPreferenceDetected();

        whenDetectingPreference();

        thenPreferenceResponseIsNoContent();
    }

    @Test
    void givenUserId_whenDetectingPreference_thenUsesResolvedStudentId() {
        givenNoPreferenceDetectedScenario();
        givenStudentIdForUser();
        givenNoPreferenceDetectedForResolvedStudent();

        whenDetectingPreference();

        thenPreferenceResponseIsNoContent();
        thenDetectPreferenceWasCalledWithResolvedStudentId();
    }

    @Test
    void givenEmptyPreferenceList_whenGettingPreferences_thenReturnsNoContent() {
        givenUserId();
        givenUserHasNoStudent();
        givenPreferencesByUser();

        whenGettingPreferences();

        thenPreferencesResponseIsNoContent();
    }

    @Test
    void givenExistingPreferences_whenGettingPreferences_thenReturnsPreferences() {
        givenUserId();
        givenUserHasNoStudent();
        givenPreferencesByUser(preference(
                randomUUID(),
                null,
                null,
                false,
                null,
                null,
                "Test",
                "Msg"
        ));

        whenGettingPreferences();

        thenPreferencesAreReturned();
        thenIntermediatePlanWasRequired();
    }

    @Test
    void givenStudentUser_whenGettingPreferences_thenRequiresSchoolLicense() {
        givenUserId();
        givenStudentId();
        givenStudentIdForUser();
        givenPreferencesByUser();

        whenGettingPreferences();

        thenStudentSchoolLicenseWasRequired();
    }

    @Test
    void givenParentPreferenceView_whenGettingPreferences_thenReturnsParentTitle() {
        givenParentPreferenceView();
        givenUserHasNoStudent();
        givenPreferencesByUser(preference(
                randomUUID(),
                childId,
                childUserId,
                true,
                "Ana",
                "Lopez",
                "Probar fruta",
                "Puede gustarle"
        ));

        whenGettingPreferences();

        thenParentPreferenceViewIsReturned();
    }

    @Test
    void givenAISuggestionResponse_whenResponding_thenDelegatesAndReturnsOk() {
        givenAISuggestionResponse();
        givenStudentIdForUser();

        whenRespondingToSuggestion();

        thenSuggestionResponseIsOk();
        thenIntermediatePlanWasRequired();
        thenStudentSchoolLicenseWasRequired();
        thenAISuggestionWasAnswered();
    }

    @Test
    void givenFavoriteProductRequest_whenAddingFavorite_thenDelegatesAndReturnsOk() {
        givenFavoriteProductRequest();

        whenAddingFavorite();

        thenFavoriteProductIsAdded();
    }

    @Test
    void givenFavoriteProductRequest_whenRemovingFavorite_thenDelegatesAndReturnsOk() {
        givenFavoriteProductRequest();

        whenRemovingFavorite();

        thenFavoriteProductIsRemoved();
    }

    @Test
    void givenFavoriteProducts_whenGettingFavorites_thenReturnsProducts() {
        givenFavoriteProductsScenario();
        givenFavoriteProducts();

        whenGettingFavorites();

        thenFavoriteProductsAreReturned();
        thenGetFavoritesWasCalled();
    }

    private void givenDetectedPreferenceScenario() {
        userId = randomUUID();
        studentId = randomUUID();
        buffetId = randomUUID();
        productId = randomUUID();
        detectedPreference = new DetectStudentPreferencesResponse(
                "Yogur con Cereales",
                "He notado que te gusta...",
                productId,
                "Frecuencia alta"
        );
    }

    private void givenNoPreferenceDetectedScenario() {
        userId = randomUUID();
        studentId = randomUUID();
        buffetId = randomUUID();
    }

    private void givenUserId() {
        userId = randomUUID();
    }

    private void givenStudentId() {
        studentId = randomUUID();
    }

    private void givenParentPreferenceView() {
        userId = randomUUID();
        childId = randomUUID();
        childUserId = randomUUID();
    }

    private void givenAISuggestionResponse() {
        userId = randomUUID();
        studentId = randomUUID();
        suggestionId = randomUUID();
    }

    private void givenFavoriteProductRequest() {
        studentId = randomUUID();
        productId = randomUUID();
    }

    private void givenFavoriteProductsScenario() {
        studentId = randomUUID();
        productId = randomUUID();
    }

    private void givenStudentIdForUser() {
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
    }

    private void givenUserHasNoStudent() {
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.empty());
    }

    private void givenPreferenceDetected() {
        when(detectStudentPreferences.execute(detectRequest()))
                .thenReturn(Optional.of(detectedPreference));
    }

    private void givenNoPreferenceDetected() {
        when(detectStudentPreferences.execute(detectRequest())).thenReturn(Optional.empty());
    }

    private void givenNoPreferenceDetectedForResolvedStudent() {
        givenNoPreferenceDetected();
    }

    private void givenPreferencesByUser(
            GetStudentPreferencesResponse.Preference... preferences) {
        when(getStudentPreferences.execute(userPreferencesRequest()))
                .thenReturn(new GetStudentPreferencesResponse(List.of(preferences)));
    }

    private void givenFavoriteProducts() {
        when(getFavoritesByStudent.execute(new GetFavoritesByStudentRequest(studentId)))
                .thenReturn(Set.of(favoriteProduct(productId)));
    }

    private void whenDetectingPreference() {
        preferenceResponse = controller.detect(userId, buffetId);
    }

    private void whenGettingPreferences() {
        preferencesResponse = controller.getPreferences(userId, false, null);
    }

    private void whenRespondingToSuggestion() {
        voidResponse = controller.respond(userId, suggestionId, true);
    }

    private void whenAddingFavorite() {
        voidResponse = controller.agregarFavorito(studentId, productId);
    }

    private void whenRemovingFavorite() {
        voidResponse = controller.removerFavorito(studentId, productId);
    }

    private void whenGettingFavorites() {
        favoriteProductsResponse = controller.obtenerFavoritos(studentId);
    }

    private void thenPreferenceResponseIsReturned() {
        assertEquals(HttpStatus.OK, preferenceResponse.getStatusCode());
        assertEquals(detectedPreference.titulo(), preferenceResponse.getBody().titulo());
    }

    private void thenPreferenceResponseIsNoContent() {
        assertEquals(HttpStatus.NO_CONTENT, preferenceResponse.getStatusCode());
    }

    private void thenDetectPreferenceWasCalledWithResolvedStudentId() {
        verify(detectStudentPreferences).execute(detectRequest());
    }

    private void thenPreferencesResponseIsNoContent() {
        assertEquals(HttpStatus.NO_CONTENT, preferencesResponse.getStatusCode());
    }

    private void thenPreferencesAreReturned() {
        assertEquals(HttpStatus.OK, preferencesResponse.getStatusCode());
        assertEquals(1, preferencesResponse.getBody().size());
    }

    private void thenParentPreferenceViewIsReturned() {
        assertEquals(HttpStatus.OK, preferencesResponse.getStatusCode());
        assertEquals("Ana Lopez: Probar fruta", preferencesResponse.getBody().get(0).titulo());
        assertEquals(childId, preferencesResponse.getBody().get(0).alumnoId());
        assertEquals(childUserId, preferencesResponse.getBody().get(0).alumnoUserId());
    }

    private void thenSuggestionResponseIsOk() {
        assertEquals(HttpStatus.OK, voidResponse.getStatusCode());
    }

    private void thenAISuggestionWasAnswered() {
        verify(respondToAISuggestion).execute(
                new RespondToAISuggestionRequest(suggestionId, true));
    }

    private void thenFavoriteProductIsAdded() {
        assertEquals(HttpStatus.OK, voidResponse.getStatusCode());
        verify(addFavoriteProduct).execute(new AddFavoriteProductRequest(studentId, productId));
    }

    private void thenFavoriteProductIsRemoved() {
        assertEquals(HttpStatus.OK, voidResponse.getStatusCode());
        verify(deleteFavoriteProduct).execute(
                new DeleteFavoriteProductRequest(studentId, productId));
    }

    private void thenFavoriteProductsAreReturned() {
        assertEquals(HttpStatus.OK, favoriteProductsResponse.getStatusCode());
        assertEquals(1, favoriteProductsResponse.getBody().size());
        assertEquals(productId, favoriteProductsResponse.getBody().get(0).id());
    }

    private void thenGetFavoritesWasCalled() {
        verify(getFavoritesByStudent).execute(new GetFavoritesByStudentRequest(studentId));
    }

    private void thenIntermediatePlanWasRequired() {
        verify(planAccessGuard).requireUserOrInheritedTutorIntermediate(userId);
    }

    private void thenStudentSchoolLicenseWasRequired() {
        verify(schoolLicenseAccessGuard).requireActiveSchoolForStudent(studentId);
    }

    private DetectStudentPreferencesRequest detectRequest() {
        return new DetectStudentPreferencesRequest(studentId, buffetId);
    }

    private GetStudentPreferencesRequest userPreferencesRequest() {
        return GetStudentPreferencesRequest.byUser(userId, false, null);
    }

    private GetStudentPreferencesResponse.Preference preference(
            UUID suggestionId,
            UUID studentId,
            UUID studentUserId,
            boolean parentView,
            String firstName,
            String lastName,
            String title,
            String message) {
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
                null,
                null,
                null,
                parentView
        );
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
