package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.KiosqueroPayoutConfig;
import com.recred.backend.domain.models.PayoutScheduleStatus;
import com.recred.backend.domain.repositories.KiosqueroPayoutConfigRepository;
import com.recred.backend.infrastructure.entities.KiosqueroEntity;
import com.recred.backend.infrastructure.entities.KiosqueroPayoutConfigEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class KiosqueroPayoutConfigRepositoryImpl implements KiosqueroPayoutConfigRepository {

    private final KiosqueroPayoutConfigJpaRepository jpaRepository;
    private final EntityManager entityManager;

    public KiosqueroPayoutConfigRepositoryImpl(
            KiosqueroPayoutConfigJpaRepository jpaRepository,
            EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public KiosqueroPayoutConfig save(KiosqueroPayoutConfig config) {
        KiosqueroPayoutConfigEntity entity = jpaRepository.findById(config.getKiosqueroId())
                .orElseGet(KiosqueroPayoutConfigEntity::new);
        
        toEntity(config, entity);
        KiosqueroPayoutConfigEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<KiosqueroPayoutConfig> findByKiosqueroId(UUID kiosqueroId) {
        return jpaRepository.findById(kiosqueroId)
                .map(KiosqueroPayoutConfigEntity::toDomain);
    }

    @Override
    public List<KiosqueroPayoutConfig> findActiveConfigsDueOnOrBefore(LocalDate date) {
        return jpaRepository.findByEstadoAndProximaEjecucionLessThanEqual(
                PayoutScheduleStatus.ACTIVE, date)
                .stream()
                .map(KiosqueroPayoutConfigEntity::toDomain)
                .collect(Collectors.toList());
    }

    private void toEntity(KiosqueroPayoutConfig domain, KiosqueroPayoutConfigEntity entity) {
        entity.setKiosqueroId(domain.getKiosqueroId());
        entity.setDestinationCvu(domain.getDestinationCvu());
        entity.setDestinationCuit(domain.getDestinationCuit());
        entity.setAccountHolderName(domain.getAccountHolderName());
        entity.setCantidadIntervalo(domain.getCantidadIntervalo());
        entity.setUnidadIntervalo(domain.getUnidadIntervalo());
        entity.setEstado(domain.getEstado());
        entity.setProximaEjecucion(domain.getProximaEjecucion());
        entity.setUltimaEjecucion(domain.getUltimaEjecucion());
        
        if (entity.getKiosquero() == null) {
            KiosqueroEntity kiosquero = entityManager.find(
                    KiosqueroEntity.class, domain.getKiosqueroId());
            if (kiosquero == null) {
                throw new IllegalStateException("Kiosquero no encontrado para el id " 
                        + domain.getKiosqueroId());
            }
            entity.setKiosquero(kiosquero);
        }
    }
}
