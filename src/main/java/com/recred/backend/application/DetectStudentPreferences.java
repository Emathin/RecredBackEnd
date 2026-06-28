package com.recred.backend.application;

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
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
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
            DetectStudentPreferencesRequest request) {
        request.validate();

        Alumno alumno = findAlumno(request.alumnoId());

        return findAnalyzableHistory(request.alumnoId())
                .flatMap(historial -> findNonEmptyAvailableStock(request.buffetId())
                        .flatMap(stockDisponible -> analyzePreference(
                                alumno,
                                historial,
                                stockDisponible
                        ).flatMap(preference -> saveIfProductIsAvailable(
                                request,
                                preference,
                                stockDisponible
                        ))));
    }

    private Alumno findAlumno(UUID alumnoId) {
        return alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + alumnoId));
    }

    private Optional<List<CompraResumen>> findAnalyzableHistory(UUID alumnoId) {
        List<CompraResumen> historial =
                purchaseRepository.findLastPurchasesByAlumno(alumnoId, PURCHASE_HISTORY_LIMIT);

        if (historial.size() < MIN_PURCHASES_FOR_ANALYSIS) {
            return Optional.empty();
        }

        return Optional.of(historial);
    }

    private Optional<List<Producto>> findNonEmptyAvailableStock(UUID buffetId) {
        List<Producto> stockDisponible = inventoryRepository.findAvailableProductsByBuffet(
                buffetId);

        if (stockDisponible.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(stockDisponible);
    }

    private Optional<PreferenciaDetectada> analyzePreference(
            Alumno alumno,
            List<CompraResumen> historial,
            List<Producto> stockDisponible
    ) {
        return preferenceAIRepository.analizarPreferencias(alumno, historial, stockDisponible);
    }

    private Optional<DetectStudentPreferencesResponse> saveIfProductIsAvailable(
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
        sugerenciaIARepository.guardar(suggestion(request, preference, productoId));
    }

    private SugerenciaIAEntity suggestion(
            DetectStudentPreferencesRequest request,
            PreferenciaDetectada preference,
            UUID productoId
    ) {
        SugerenciaIAEntity sugerencia = new SugerenciaIAEntity();
        sugerencia.setTipo(TipoSugerenciaIA.PREFERENCIA_DETECTADA);
        sugerencia.setTitulo(preference.titulo());
        sugerencia.setMensaje(preference.mensaje());
        sugerencia.setAceptada(null);
        sugerencia.setMetadata(metadata(preference));
        sugerencia.setAlumnoEntity(alumnoEntity(request.alumnoId()));
        sugerencia.setBuffetEntity(buffetEntity(request.buffetId()));
        sugerencia.setProductoEntity(productoEntity(productoId));
        return sugerencia;
    }

    private Map<String, Object> metadata(PreferenciaDetectada preference) {
        return Map.of(
                "productoId", preference.productoId(),
                "razonIA", preference.razonIA()
        );
    }

    private AlumnoEntity alumnoEntity(UUID alumnoId) {
        AlumnoEntity alumnoEntity = new AlumnoEntity();
        alumnoEntity.setId(alumnoId);
        return alumnoEntity;
    }

    private BuffetEntity buffetEntity(UUID buffetId) {
        BuffetEntity buffetEntity = new BuffetEntity();
        buffetEntity.setId(buffetId);
        return buffetEntity;
    }

    private ProductoEntity productoEntity(UUID productoId) {
        ProductoEntity productoEntity = new ProductoEntity();
        productoEntity.setId(productoId);
        return productoEntity;
    }
}
