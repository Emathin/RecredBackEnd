package com.recred.backend.unit.application;

import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.DetectStudentPreferencesRequest;
import com.recred.backend.application.DetectStudentPreferencesResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository.CompraResumen;
import com.recred.backend.domain.repositories.PreferenceAIRepository.PreferenciaDetectada;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetectStudentPreferencesTest {

    private static final String TITLE = "Title";
    private static final String MESSAGE = "Message";
    private static final String AI_REASON = "Reason";
    private static final int PURCHASE_HISTORY_LIMIT = 20;

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private PreferenceAIRepository preferenceAIRepository;
    @Mock
    private SugerenciaIARepository sugerenciaIARepository;
    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private InventoryRepository inventoryRepository;

    @Captor
    private ArgumentCaptor<SugerenciaIA> suggestionCaptor;

    private DetectStudentPreferences detectStudentPreferences;
    private DetectStudentPreferencesRequest detectPreferencesRequest;
    private Optional<DetectStudentPreferencesResponse> detectPreferencesResponse;
    private Throwable thrownException;
    private UUID studentId;
    private UUID buffetId;
    private Alumno student;
    private Producto product;
    private List<Producto> availableStock;
    private List<CompraResumen> purchaseHistory;
    private PreferenciaDetectada detectedPreference;

    @BeforeEach
    void setUp() {
        detectStudentPreferences = new DetectStudentPreferences(
                alumnoRepository,
                preferenceAIRepository,
                sugerenciaIARepository,
                purchaseRepository,
                inventoryRepository
        );
    }

    @Test
    void givenValidPreferenceDetected_whenDetectingPreferences_thenSavesSuggestion() {
        givenValidPreferenceDetectionScenario();
        givenPreferenceDetectedForAvailableProduct();

        whenDetectStudentPreferences();

        thenDetectedPreferenceWasReturned();
        thenSuggestionWasSaved();
    }

    @Test
    void givenInsufficientPurchaseHistory_whenDetectingPreferences_thenReturnsEmpty() {
        givenStudentExists();
        givenPurchaseHistory(insufficientHistory());

        whenDetectStudentPreferences();

        thenNoPreferenceWasReturned();
        thenPreferenceWasNotAnalyzed();
        thenSuggestionWasNotSaved();
    }

    @Test
    void givenNoAvailableStock_whenDetectingPreferences_thenReturnsEmpty() {
        givenStudentExists();
        givenPurchaseHistory(sufficientHistory());
        givenAvailableStock(List.of());

        whenDetectStudentPreferences();

        thenNoPreferenceWasReturned();
        thenPreferenceWasNotAnalyzed();
        thenSuggestionWasNotSaved();
    }

    @Test
    void givenDetectedProductOutsideStock_whenDetectingPreferences_thenReturnsEmpty() {
        givenValidPreferenceDetectionScenario();
        givenPreferenceDetectedForUnavailableProduct();

        whenDetectStudentPreferences();

        thenNoPreferenceWasReturned();
        thenSuggestionWasNotSaved();
    }

    @Test
    void givenAiDoesNotDetectPreference_whenDetectingPreferences_thenReturnsEmpty() {
        givenValidPreferenceDetectionScenario();
        givenNoPreferenceDetected();

        whenDetectStudentPreferences();

        thenNoPreferenceWasReturned();
        thenSuggestionWasNotSaved();
    }

    @Test
    void givenMissingStudent_whenDetectingPreferences_thenThrowsException() {
        givenRequest();
        givenStudentDoesNotExist();

        whenDetectStudentPreferencesFails(RuntimeException.class);

        thenErrorMessageIs("Student not found: " + studentId);
    }

    @Test
    void givenUnexpectedRepositoryError_whenDetectingPreferences_thenRethrowsException() {
        givenRequest();
        givenStudentLookupFails();

        whenDetectStudentPreferencesFails(RuntimeException.class);

        thenErrorMessageIs("Database error");
    }

    @Test
    void givenNullStudentId_whenDetectingPreferences_thenThrowsBeforeRepositories() {
        detectPreferencesRequest = new DetectStudentPreferencesRequest(null, randomUUID());

        whenDetectStudentPreferencesFails(IllegalArgumentException.class);

        thenErrorMessageIs("El alumno es obligatorio");
        thenStudentWasNotRead();
    }

    @Test
    void givenNullBuffetId_whenDetectingPreferences_thenThrowsBeforeRepositories() {
        detectPreferencesRequest = new DetectStudentPreferencesRequest(randomUUID(), null);

        whenDetectStudentPreferencesFails(IllegalArgumentException.class);

        thenErrorMessageIs("El buffet es obligatorio");
        thenStudentWasNotRead();
    }

    private void givenValidPreferenceDetectionScenario() {
        givenStudentExists();
        givenPurchaseHistory(sufficientHistory());
        givenAvailableStockWithProduct();
    }

    private void givenRequest() {
        studentId = randomUUID();
        buffetId = randomUUID();
        detectPreferencesRequest = new DetectStudentPreferencesRequest(studentId, buffetId);
    }

    private void givenStudentExists() {
        givenRequest();
        student = student(studentId);
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    private void givenStudentDoesNotExist() {
        when(alumnoRepository.findById(studentId)).thenReturn(Optional.empty());
    }

    private void givenStudentLookupFails() {
        when(alumnoRepository.findById(studentId))
                .thenThrow(new RuntimeException("Database error"));
    }

    private void givenPurchaseHistory(List<CompraResumen> history) {
        purchaseHistory = history;
        when(purchaseRepository.findLastPurchasesByAlumno(studentId, PURCHASE_HISTORY_LIMIT))
                .thenReturn(history);
    }

    private void givenAvailableStockWithProduct() {
        product = product();
        givenAvailableStock(List.of(product));
    }

    private void givenAvailableStock(List<Producto> stock) {
        availableStock = stock;
        when(inventoryRepository.findAvailableProductsByBuffet(buffetId)).thenReturn(stock);
    }

    private void givenPreferenceDetectedForAvailableProduct() {
        detectedPreference = detectedPreference(product.getId());
        givenPreferenceDetected(detectedPreference);
    }

    private void givenPreferenceDetectedForUnavailableProduct() {
        detectedPreference = detectedPreference(randomUUID());
        givenPreferenceDetected(detectedPreference);
    }

    private void givenPreferenceDetected(PreferenciaDetectada preference) {
        when(preferenceAIRepository.analizarPreferencias(
                student,
                purchaseHistory,
                availableStock))
                .thenReturn(Optional.of(preference));
    }

    private void givenNoPreferenceDetected() {
        when(preferenceAIRepository.analizarPreferencias(
                student,
                purchaseHistory,
                availableStock))
                .thenReturn(Optional.empty());
    }

    private void whenDetectStudentPreferences() {
        detectPreferencesResponse = detectStudentPreferences.execute(detectPreferencesRequest);
    }

    private <T extends Throwable> void whenDetectStudentPreferencesFails(
            Class<T> expectedException) {
        Executable execution = () -> detectStudentPreferences.execute(detectPreferencesRequest);
        thrownException = assertThrows(expectedException, execution);
    }

    private void thenDetectedPreferenceWasReturned() {
        assertTrue(detectPreferencesResponse.isPresent());
        assertEquals(TITLE, detectPreferencesResponse.orElseThrow().titulo());
        assertEquals(product.getId(), detectPreferencesResponse.orElseThrow().productoId());
    }

    private void thenNoPreferenceWasReturned() {
        assertTrue(detectPreferencesResponse.isEmpty());
    }

    private void thenSuggestionWasSaved() {
        verify(sugerenciaIARepository).save(suggestionCaptor.capture());
        SugerenciaIA savedSuggestion = suggestionCaptor.getValue();
        assertEquals(TipoSugerenciaIA.PREFERENCIA_DETECTADA, savedSuggestion.getTipo());
        assertEquals(TITLE, savedSuggestion.getTitulo());
        assertEquals(MESSAGE, savedSuggestion.getMensaje());
        assertEquals(product.getId(), savedSuggestion.getProductoId());
        assertEquals(studentId, savedSuggestion.getAlumnoId());
        assertEquals(buffetId, savedSuggestion.getBuffetId());
        assertEquals(product.getId().toString(), savedSuggestion.getMetadata().get("productoId"));
        assertEquals(AI_REASON, savedSuggestion.getMetadata().get("razonIA"));
    }

    private void thenPreferenceWasNotAnalyzed() {
        verify(preferenceAIRepository, never()).analizarPreferencias(any(), any(), any());
    }

    private void thenSuggestionWasNotSaved() {
        verify(sugerenciaIARepository, never()).save(any());
    }

    private void thenStudentWasNotRead() {
        verify(alumnoRepository, never()).findById(any());
    }

    private void thenErrorMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private Alumno student(UUID id) {
        Alumno student = new Alumno();
        student.setId(id);
        return student;
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(randomUUID());
        return product;
    }

    private List<CompraResumen> sufficientHistory() {
        return List.of(
                purchase("A"),
                purchase("B"),
                purchase("C")
        );
    }

    private List<CompraResumen> insufficientHistory() {
        return List.of(
                purchase("A"),
                purchase("B")
        );
    }

    private CompraResumen purchase(String productName) {
        return new CompraResumen(productName, "C", LocalDateTime.of(2026, 7, 6, 10, 0));
    }

    private PreferenciaDetectada detectedPreference(UUID productId) {
        return new PreferenciaDetectada(
                TITLE,
                MESSAGE,
                productId.toString(),
                AI_REASON
        );
    }
}
