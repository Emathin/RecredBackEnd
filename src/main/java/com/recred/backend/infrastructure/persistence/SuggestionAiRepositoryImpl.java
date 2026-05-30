package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.PurchaseRecommendationResult;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.SuggestionAiRepository;
import com.recred.backend.infrastructure.entities.AIEventEntity;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.NotificacionEntity;
import com.recred.backend.infrastructure.entities.ProductoEntity;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class SuggestionAiRepositoryImpl implements SuggestionAiRepository {

    private final EntityManager entityManager;
    private final SpringDataSuggestionAiRepository repository;

    public SuggestionAiRepositoryImpl(EntityManager entityManager, SpringDataSuggestionAiRepository repository) {
        this.entityManager = entityManager;
        this.repository = repository;
    }

    @Override
    public SugerenciaIAEntity save(UUID eventID, PurchaseRecommendationResult result) {
        SugerenciaIAEntity entity = new SugerenciaIAEntity();
        entity.setAceptada(false);
        entity.setTipo(TipoSugerenciaIA.valueOf(result.getAiAnalysis().get().purchaseDays()));
        entity.setEventoIaEntity(entityManager.getReference(AIEventEntity.class, eventID));
        entity.setAlumnoEntity(
                entityManager.getReference(AlumnoEntity.class, result.getAlumnoId()));
        entity.setProductoEntity(
                entityManager.getReference(ProductoEntity.class,
                        result.getAiAnalysis().get().product_id()));
        entity.setTitulo(result.getAiAnalysis().get().summary());
        entity.setMensaje("Mañana es " + result.getAiAnalysis().get().purchaseDays() +
                " Comprate tu " +result.getAiAnalysis().get().productName());
        return repository.save(entity);


    }

    @Override
    public List<NotificacionEntity> findByTipo(String tomorrow) {
        return repository.findByTipo(tomorrow);
    }
}
