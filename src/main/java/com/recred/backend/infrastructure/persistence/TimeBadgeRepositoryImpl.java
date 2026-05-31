package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.TimeBadge;
import com.recred.backend.domain.repositories.TimeBadgeRepository;
import com.recred.backend.infrastructure.entities.SchoolEntity;
import com.recred.backend.infrastructure.entities.ScheduleEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TimeBadgeRepositoryImpl implements TimeBadgeRepository {

    private final SpringDataFranjaHorariaRepository springDataRepository;

    public TimeBadgeRepositoryImpl(SpringDataFranjaHorariaRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public List<TimeBadge> findAllByColegioId(UUID colegioId) {
        return springDataRepository.findByColegioEntityIdAndActivoTrue(colegioId)
                .stream()
                .map(ScheduleEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public TimeBadge save(TimeBadge timeBadge) {
        ScheduleEntity entityExistente = null;

        if (timeBadge.getId() != null) {
            entityExistente = springDataRepository.findById(timeBadge.getId())
                    .orElse(new ScheduleEntity());
        } else {
            entityExistente = new ScheduleEntity();
        }

        ScheduleEntity entityLista = toEntity(timeBadge, entityExistente);
        ScheduleEntity guardada = springDataRepository.save(entityLista);

        return guardada.toDomain();
    }

    private ScheduleEntity toEntity(TimeBadge modelo, ScheduleEntity entity) {
        if (entity.getId() == null) {
            entity.setId(modelo.getId());
        }

        entity.setDescription(modelo.getDescription());
        entity.setStartTime(modelo.getStartTime());
        entity.setEndTime(modelo.getEndTime());
        entity.setActive(modelo.isActive());

        if (modelo.getSchoolId() != null) {
            SchoolEntity colegio = new SchoolEntity();
            colegio.setId(modelo.getSchoolId());
            entity.setSchoolEntity(colegio);
        }

        return entity;
    }
}