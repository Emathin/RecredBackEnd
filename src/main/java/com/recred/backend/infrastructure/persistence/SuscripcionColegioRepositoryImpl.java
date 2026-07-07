package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.SuscripcionColegio;
import com.recred.backend.domain.repositories.SuscripcionColegioRepository;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.SuscripcionColegioEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SuscripcionColegioRepositoryImpl implements SuscripcionColegioRepository {

    private final SpringDataSuscripcionColegioRepository springDataRepository;

    public SuscripcionColegioRepositoryImpl(
            SpringDataSuscripcionColegioRepository springDataRepository
    ) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SuscripcionColegio> findActiveByColegioId(UUID colegioId) {
        return springDataRepository
                .findFirstByColegioEntityIdAndEstadoOrderByFechaInicioDesc(
                        colegioId,
                        EstadoSuscripcion.ACTIVA)
                .map(SuscripcionColegioEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuscripcionColegio> findByColegioId(UUID colegioId) {
        return springDataRepository.findByColegioEntityIdOrderByFechaInicioDesc(colegioId)
                .stream()
                .map(SuscripcionColegioEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public SuscripcionColegio save(SuscripcionColegio subscription) {
        SuscripcionColegioEntity entity = existingEntity(subscription);
        SuscripcionColegioEntity saved = springDataRepository.save(toEntity(subscription, entity));
        return saved.toDomain();
    }

    private SuscripcionColegioEntity existingEntity(SuscripcionColegio subscription) {
        if (subscription.getId() == null) {
            return new SuscripcionColegioEntity();
        }

        return springDataRepository.findById(subscription.getId())
                .orElse(new SuscripcionColegioEntity());
    }

    private SuscripcionColegioEntity toEntity(
            SuscripcionColegio subscription,
            SuscripcionColegioEntity entity
    ) {
        if (entity.getId() == null) {
            entity.setId(subscription.getId());
        }

        entity.setColegioEntity(schoolReference(subscription.getColegioId()));
        entity.setPrecio(subscription.getPrecio());
        entity.setMoneda(subscription.getMoneda());
        entity.setEstado(subscription.getEstado());
        entity.setFechaInicio(subscription.getFechaInicio());
        entity.setFechaVencimiento(subscription.getFechaVencimiento());
        entity.setFechaGraciaHasta(subscription.getFechaGraciaHasta());
        entity.setCreadoEn(subscription.getCreadoEn());
        entity.setActualizadoEn(subscription.getActualizadoEn());
        return entity;
    }

    private ColegioEntity schoolReference(UUID colegioId) {
        ColegioEntity entity = new ColegioEntity();
        entity.setId(colegioId);
        return entity;
    }
}
