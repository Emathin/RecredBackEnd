package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
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

    @Transactional
    public Optional<PreferenciaDetectada> execute(UUID alumnoId, UUID buffetId) {
        try {
            Alumno alumno = alumnoRepository.findById(alumnoId)
                    .orElseThrow(() -> new RuntimeException("Student not found: " + alumnoId));

            List<PreferenceAIRepository.CompraResumen> historial =
                    purchaseRepository.findLastPurchasesByAlumno(alumnoId, 20);

            if (historial.size() < MIN_PURCHASES_FOR_ANALYSIS) {
                return Optional.empty();
            }

            List<Producto> stockDisponible =
                    inventoryRepository.findAvailableProductsByBuffet(buffetId);

            if (stockDisponible.isEmpty()) {
                return Optional.empty();
            }

            Optional<PreferenciaDetectada> resultado = 
                    preferenceAIRepository.analizarPreferencias(alumno, historial, stockDisponible);

            resultado = resultado.filter(preferencia -> 
                stockDisponible.stream()
                        .anyMatch(p -> p.getId().toString().equals(preferencia.productoId()))
            );

            resultado.ifPresent(preferencia -> {
                SugerenciaIAEntity sugerencia = new SugerenciaIAEntity();
                sugerencia.setTipo(TipoSugerenciaIA.PREFERENCIA_DETECTADA);
                sugerencia.setTitulo(preferencia.titulo());
                sugerencia.setMensaje(preferencia.mensaje());
                sugerencia.setAceptada(null);

                sugerencia.setMetadata(Map.of(
                        "productoId", preferencia.productoId(),
                        "razonIA", preferencia.razonIA()
                ));

                AlumnoEntity alumnoEntity = new AlumnoEntity();
                alumnoEntity.setId(alumnoId);
                sugerencia.setAlumnoEntity(alumnoEntity);

                BuffetEntity buffetEntity = new BuffetEntity();
                buffetEntity.setId(buffetId);
                sugerencia.setBuffetEntity(buffetEntity);

                ProductoEntity productoEntity = new ProductoEntity();
                productoEntity.setId(UUID.fromString(preferencia.productoId()));
                sugerencia.setProductoEntity(productoEntity);

                sugerenciaIARepository.savePreferenceDetection(sugerencia);
            });

            return resultado;

        } catch (Exception e) {
            throw e;
        }
    }
}
