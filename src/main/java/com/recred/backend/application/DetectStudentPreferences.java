package com.recred.backend.application;

import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.IaSuggestionType;
import com.recred.backend.domain.repositories.StudentRepository;
import com.recred.backend.domain.repositories.InventoryRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository;
import com.recred.backend.domain.repositories.PreferenceAIRepository.DetectedPreference;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.AISuggestionRepository;
import com.recred.backend.infrastructure.entities.StudentEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.ProductEntity;
import com.recred.backend.infrastructure.entities.AISuggestionEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Transactional
public class DetectStudentPreferences {

    private static final int MIN_PURCHASES_FOR_ANALYSIS = 3;
    private final StudentRepository studentRepository;
    private final PreferenceAIRepository preferenceAIRepository;
    private final AISuggestionRepository AISuggestionRepository;
    private final PurchaseRepository purchaseRepository;
    private final InventoryRepository inventoryRepository;

    public DetectStudentPreferences(
            StudentRepository studentRepository,
            PreferenceAIRepository preferenceAIRepository,
            AISuggestionRepository AISuggestionRepository,
            PurchaseRepository purchaseRepository,
            InventoryRepository inventoryRepository) {
        this.studentRepository = studentRepository;
        this.preferenceAIRepository = preferenceAIRepository;
        this.AISuggestionRepository = AISuggestionRepository;
        this.purchaseRepository = purchaseRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public Optional<DetectedPreference> execute(UUID alumnoId, UUID buffetId) {
        try {
            Student student = studentRepository.findById(alumnoId)
                    .orElseThrow(() -> new RuntimeException("Student not found: " + alumnoId));

            List<PreferenceAIRepository.PurchaseSummary> historial =
                    purchaseRepository.findLastPurchasesByStudent(alumnoId, 20);

            if (historial.size() < MIN_PURCHASES_FOR_ANALYSIS) {
                return Optional.empty();
            }

            List<Product> stockDisponible =
                    inventoryRepository.findAvailableProductsByBuffet(buffetId);

            if (stockDisponible.isEmpty()) {
                return Optional.empty();
            }

            Optional<DetectedPreference> resultado =
                    preferenceAIRepository.analizarPreferencias(student, historial, stockDisponible);

            resultado = resultado.filter(preferencia -> 
                stockDisponible.stream()
                        .anyMatch(p -> p.getId().toString().equals(preferencia.productoId()))
            );

            resultado.ifPresent(preferencia -> {
                AISuggestionEntity sugerencia = new AISuggestionEntity();
                sugerencia.setType(IaSuggestionType.PREFERENCIA_DETECTADA);
                sugerencia.setTittle(preferencia.tittle());
                sugerencia.setMessage(preferencia.message());
                sugerencia.setAccepted(null);

                sugerencia.setMetadata(Map.of(
                        "productId", preferencia.productoId(),
                        "razonIA", preferencia.razonIA()
                ));

                StudentEntity studentEntity = new StudentEntity();
                studentEntity.setId(alumnoId);
                sugerencia.setStudentEntity(studentEntity);

                BuffetEntity buffetEntity = new BuffetEntity();
                buffetEntity.setId(buffetId);
                sugerencia.setBuffetEntity(buffetEntity);

                ProductEntity productEntity = new ProductEntity();
                productEntity.setId(UUID.fromString(preferencia.productoId()));
                sugerencia.setProductEntity(productEntity);

                AISuggestionRepository.save(sugerencia);
            });

            return resultado;

        } catch (Exception e) {
            throw e;
        }
    }
}
