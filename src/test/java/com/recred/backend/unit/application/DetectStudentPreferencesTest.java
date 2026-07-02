package com.recred.backend.unit.application;

import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.DetectStudentPreferencesRequest;
import com.recred.backend.application.DetectStudentPreferencesResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository.CompraResumen;
import com.recred.backend.domain.repositories.PreferenceAIRepository.PreferenciaDetectada;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    private DetectStudentPreferences detectStudentPreferences;

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
    void executeAndSaveAISuggestionWhenValidPreferenceIsDetected() {
        TestContext context = context();
        Producto producto = product();
        List<Producto> stock = List.of(producto);
        List<CompraResumen> historial = sufficientHistory();
        PreferenciaDetectada preference = detectedPreference(producto.getId());

        givenAlumnoExists(context);
        givenPurchaseHistory(context, historial);
        givenAvailableStock(context, stock);
        givenPreferenceDetected(context.alumno(), historial, stock, preference);

        Optional<DetectStudentPreferencesResponse> result =
                detectStudentPreferences.execute(request(context));

        SugerenciaIAEntity saved = savedSuggestion();
        assertTrue(result.isPresent());
        assertEquals(TITLE, result.orElseThrow().titulo());
        assertEquals(producto.getId(), result.orElseThrow().productoId());
        assertSavedSuggestion(saved, context, producto.getId());
    }

    @Test
    void abortDetectionWhenInsufficientPurchaseHistory() {
        TestContext context = context();

        givenAlumnoExists(context);
        givenPurchaseHistory(context, insufficientHistory());

        Optional<DetectStudentPreferencesResponse> result =
                detectStudentPreferences.execute(request(context));

        assertTrue(result.isEmpty());
        assertPreferenceWasNotAnalyzed();
        assertSuggestionWasNotSaved();
    }

    @Test
    void discardAIResultWhenProductIdIsNotInAvailableStock() {
        TestContext context = context();
        Producto availableProduct = product();
        List<Producto> stock = List.of(availableProduct);
        List<CompraResumen> historial = sufficientHistory();
        PreferenciaDetectada preference = detectedPreference(UUID.randomUUID());

        givenAlumnoExists(context);
        givenPurchaseHistory(context, historial);
        givenAvailableStock(context, stock);
        givenPreferenceDetected(context.alumno(), historial, stock, preference);

        Optional<DetectStudentPreferencesResponse> result =
                detectStudentPreferences.execute(request(context));

        assertTrue(result.isEmpty());
        assertSuggestionWasNotSaved();
    }

    @Test
    void abortDetectionWhenNoStockIsAvailableInBuffet() {
        TestContext context = context();

        givenAlumnoExists(context);
        givenPurchaseHistory(context, sufficientHistory());
        givenAvailableStock(context, List.of());

        Optional<DetectStudentPreferencesResponse> result =
                detectStudentPreferences.execute(request(context));

        assertTrue(result.isEmpty());
        assertPreferenceWasNotAnalyzed();
        assertSuggestionWasNotSaved();
    }

    @Test
    void doNothingWhenAINoPreferenceDetected() {
        TestContext context = context();
        Producto producto = product();
        List<Producto> stock = List.of(producto);
        List<CompraResumen> historial = sufficientHistory();

        givenAlumnoExists(context);
        givenPurchaseHistory(context, historial);
        givenAvailableStock(context, stock);
        givenNoPreferenceDetected(context.alumno(), historial, stock);

        Optional<DetectStudentPreferencesResponse> result =
                detectStudentPreferences.execute(request(context));

        assertTrue(result.isEmpty());
        assertSuggestionWasNotSaved();
    }

    @Test
    void throwExceptionWhenTargetStudentDoesNotExist() {
        TestContext context = context();
        when(alumnoRepository.findById(context.studentId())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> detectStudentPreferences.execute(request(context)));
    }

    @Test
    void rethrowExceptionWhenUnexpectedErrorOccursInProcess() {
        TestContext context = context();
        when(alumnoRepository.findById(context.studentId()))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class,
                () -> detectStudentPreferences.execute(request(context)));
    }

    @Test
    void rejectNullAlumnoIdBeforeReadingRepositories() {
        DetectStudentPreferencesRequest request =
                new DetectStudentPreferencesRequest(null, UUID.randomUUID());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> detectStudentPreferences.execute(request));

        assertEquals("El alumno es obligatorio", exception.getMessage());
        verify(alumnoRepository, never()).findById(any());
    }

    @Test
    void rejectNullBuffetIdBeforeReadingRepositories() {
        DetectStudentPreferencesRequest request =
                new DetectStudentPreferencesRequest(UUID.randomUUID(), null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> detectStudentPreferences.execute(request));

        assertEquals("El buffet es obligatorio", exception.getMessage());
        verify(alumnoRepository, never()).findById(any());
    }

    private TestContext context() {
        UUID studentId = UUID.randomUUID();
        return new TestContext(studentId, UUID.randomUUID(), alumno(studentId));
    }

    private DetectStudentPreferencesRequest request(TestContext context) {
        return new DetectStudentPreferencesRequest(context.studentId(), context.buffetId());
    }

    private Alumno alumno(UUID id) {
        Alumno alumno = new Alumno();
        alumno.setId(id);
        return alumno;
    }

    private Producto product() {
        Producto producto = new Producto();
        producto.setId(UUID.randomUUID());
        return producto;
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
        return new CompraResumen(productName, "C", LocalDateTime.now());
    }

    private PreferenciaDetectada detectedPreference(UUID productId) {
        return new PreferenciaDetectada(
                TITLE,
                MESSAGE,
                productId.toString(),
                AI_REASON
        );
    }

    private void givenAlumnoExists(TestContext context) {
        when(alumnoRepository.findById(context.studentId()))
                .thenReturn(Optional.of(context.alumno()));
    }

    private void givenPurchaseHistory(TestContext context, List<CompraResumen> historial) {
        when(purchaseRepository.findLastPurchasesByAlumno(
                context.studentId(),
                PURCHASE_HISTORY_LIMIT))
                .thenReturn(historial);
    }

    private void givenAvailableStock(TestContext context, List<Producto> stock) {
        when(inventoryRepository.findAvailableProductsByBuffet(context.buffetId()))
                .thenReturn(stock);
    }

    private void givenPreferenceDetected(
            Alumno alumno,
            List<CompraResumen> historial,
            List<Producto> stock,
            PreferenciaDetectada preference
    ) {
        when(preferenceAIRepository.analizarPreferencias(alumno, historial, stock))
                .thenReturn(Optional.of(preference));
    }

    private void givenNoPreferenceDetected(
            Alumno alumno,
            List<CompraResumen> historial,
            List<Producto> stock
    ) {
        when(preferenceAIRepository.analizarPreferencias(alumno, historial, stock))
                .thenReturn(Optional.empty());
    }

    private SugerenciaIAEntity savedSuggestion() {
        ArgumentCaptor<SugerenciaIAEntity> captor =
                ArgumentCaptor.forClass(SugerenciaIAEntity.class);
        verify(sugerenciaIARepository).guardar(captor.capture());
        return captor.getValue();
    }

    private void assertSavedSuggestion(
            SugerenciaIAEntity saved,
            TestContext context,
            UUID productId
    ) {
        assertEquals(TipoSugerenciaIA.PREFERENCIA_DETECTADA, saved.getTipo());
        assertEquals(TITLE, saved.getTitulo());
        assertEquals(MESSAGE, saved.getMensaje());
        assertEquals(productId, saved.getProductoEntity().getId());
        assertEquals(context.studentId(), saved.getAlumnoEntity().getId());
        assertEquals(context.buffetId(), saved.getBuffetEntity().getId());
        assertEquals(productId.toString(), saved.getMetadata().get("productoId"));
        assertEquals(AI_REASON, saved.getMetadata().get("razonIA"));
    }

    private void assertPreferenceWasNotAnalyzed() {
        verify(preferenceAIRepository, never()).analizarPreferencias(any(), any(), any());
    }

    private void assertSuggestionWasNotSaved() {
        verify(sugerenciaIARepository, never()).guardar(any());
    }

    private record TestContext(UUID studentId, UUID buffetId, Alumno alumno) {
    }
}
