package com.recred.backend.application;

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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Transactional
public class DetectStudentPreferences {

    private static final int PURCHASE_HISTORY_LIMIT = 20;
    private static final int MIN_PURCHASES_FOR_ANALYSIS = 3;

    private final AlumnoRepository alumnoRepository;
    private final PreferenceAIRepository preferenceAIRepository;
    private final SugerenciaIARepository sugerenciaIARepository;
    private final PurchaseRepository purchaseRepository;
    private final InventoryRepository inventoryRepository;

    public DetectStudentPreferences(
            AlumnoRepository alumnoRepository,
            PreferenceAIRepository preferenceAIRepository,
            SugerenciaIARepository sugerenciaIARepository,
            PurchaseRepository purchaseRepository,
            InventoryRepository inventoryRepository) {
        this.alumnoRepository = alumnoRepository;
        this.preferenceAIRepository = preferenceAIRepository;
        this.sugerenciaIARepository = sugerenciaIARepository;
        this.purchaseRepository = purchaseRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public Optional<DetectStudentPreferencesResponse> execute(
            DetectStudentPreferencesRequest detectPreferencesRequest) {
        detectPreferencesRequest.ensureRequiredFieldsPresent();
        Optional<PreferenceDetectionContext> context = detectionContext(
                detectPreferencesRequest);

        return context.flatMap(preferenceContext -> detectAndSavePreference(
                detectPreferencesRequest,
                preferenceContext));
    }

    private Optional<PreferenceDetectionContext> detectionContext(
            DetectStudentPreferencesRequest request
    ) {
        Alumno alumno = findAlumno(request.alumnoId());
        List<CompraResumen> purchaseHistory = findPurchaseHistory(request.alumnoId());
        if (!hasEnoughHistory(purchaseHistory)) {
            return Optional.empty();
        }

        List<Producto> availableStock = findAvailableStock(request.buffetId());
        if (availableStock.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new PreferenceDetectionContext(
                alumno,
                purchaseHistory,
                availableStock
        ));
    }

    private Optional<DetectStudentPreferencesResponse> detectAndSavePreference(
            DetectStudentPreferencesRequest request,
            PreferenceDetectionContext context
    ) {
        return analyzePreference(
                context.alumno(),
                context.purchaseHistory(),
                context.availableStock())
                .flatMap(preference -> saveDetectedPreferenceIfAvailable(
                        request,
                        preference,
                        context.availableStock()
                ));
    }

    private Alumno findAlumno(UUID alumnoId) {
        return alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + alumnoId));
    }

    private List<CompraResumen> findPurchaseHistory(UUID alumnoId) {
        return purchaseRepository.findLastPurchasesByAlumno(alumnoId, PURCHASE_HISTORY_LIMIT);
    }

    private boolean hasEnoughHistory(List<CompraResumen> historial) {
        return historial.size() >= MIN_PURCHASES_FOR_ANALYSIS;
    }

    private List<Producto> findAvailableStock(UUID buffetId) {
        return inventoryRepository.findAvailableProductsByBuffet(buffetId);
    }

    private Optional<PreferenciaDetectada> analyzePreference(
            Alumno alumno,
            List<CompraResumen> historial,
            List<Producto> stockDisponible
    ) {
        return preferenceAIRepository.analizarPreferencias(alumno, historial, stockDisponible);
    }

    private Optional<DetectStudentPreferencesResponse> saveDetectedPreferenceIfAvailable(
            DetectStudentPreferencesRequest request,
            PreferenciaDetectada preference,
            List<Producto> stockDisponible
    ) {
        return availableProductId(preference, stockDisponible)
                .map(productoId -> saveAndBuildResponse(request, preference, productoId));
    }

    private Optional<UUID> availableProductId(
            PreferenciaDetectada preference,
            List<Producto> stockDisponible
    ) {
        UUID productoId = UUID.fromString(preference.productoId());

        return stockDisponible.stream()
                .map(Producto::getId)
                .filter(productoId::equals)
                .findFirst();
    }

    private DetectStudentPreferencesResponse saveAndBuildResponse(
            DetectStudentPreferencesRequest request,
            PreferenciaDetectada preference,
            UUID productoId
    ) {
        saveSuggestion(request, preference, productoId);
        return DetectStudentPreferencesResponse.fromDetectedPreference(
                preference,
                productoId
        );
    }

    private void saveSuggestion(
            DetectStudentPreferencesRequest request,
            PreferenciaDetectada preference,
            UUID productoId
    ) {
        sugerenciaIARepository.save(suggestion(request, preference, productoId));
    }

    private SugerenciaIA suggestion(
            DetectStudentPreferencesRequest request,
            PreferenciaDetectada preference,
            UUID productoId
    ) {
        return SugerenciaIA.builder()
                .tipo(TipoSugerenciaIA.PREFERENCIA_DETECTADA)
                .titulo(preference.titulo())
                .mensaje(preference.mensaje())
                .metadata(metadata(preference))
                .alumnoId(request.alumnoId())
                .buffetId(request.buffetId())
                .productoId(productoId)
                .build();
    }

    private Map<String, Object> metadata(PreferenciaDetectada preference) {
        return Map.of(
                "productoId", preference.productoId(),
                "razonIA", preference.razonIA()
        );
    }

    private record PreferenceDetectionContext(
            Alumno alumno,
            List<CompraResumen> purchaseHistory,
            List<Producto> availableStock
    ) {
    }
}
