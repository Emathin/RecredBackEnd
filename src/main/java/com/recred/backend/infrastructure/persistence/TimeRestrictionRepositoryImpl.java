package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.TimeRestriction;
import com.recred.backend.domain.repositories.TimeRestrictionRepository;
import com.recred.backend.infrastructure.entities.StudentEntity;
import com.recred.backend.infrastructure.entities.ProductCategoryEntity;
import com.recred.backend.infrastructure.entities.TypeClassificationEntity;
import com.recred.backend.infrastructure.entities.ScheduleEntity;
import com.recred.backend.infrastructure.entities.TimeRestrictionEntity;
import com.recred.backend.infrastructure.entities.UserEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TimeRestrictionRepositoryImpl implements
        TimeRestrictionRepository {

    private final SpringDataRestriccionHorariaRepository springDataRepository;

    public TimeRestrictionRepositoryImpl
            (SpringDataRestriccionHorariaRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public TimeRestriction save(TimeRestriction restriccion) {
        TimeRestrictionEntity entity = restriccion.getId() != null
                ? springDataRepository.findById(restriccion.getId())
                .orElse(new TimeRestrictionEntity())
                : new TimeRestrictionEntity();

        toEntity(restriccion, entity);
        TimeRestrictionEntity entidadGuardada = springDataRepository.save(entity);

        return entidadGuardada.toDomain();
    }

    @Override
    public Optional<TimeRestriction> findById(UUID id) {
        return springDataRepository.findById(id)
                .map(TimeRestrictionEntity::toDomain);
    }

    @Override
    public List<TimeRestriction> findAllByAlumnoIdAndActiva
            (UUID alumnoId, boolean activa) {
        return springDataRepository.findAllByAlumnoEntityIdAndActiva(alumnoId, activa)
                .stream()
                .map(TimeRestrictionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TimeRestriction> findAllByAlumnoId(UUID alumnoId) {
        return springDataRepository.findAllByAlumnoEntityId(alumnoId).stream()
                .map(TimeRestrictionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        springDataRepository.deleteById(id);
    }

    private void toEntity(TimeRestriction domain,
                          TimeRestrictionEntity entity) {
        if (entity.getId() == null) {
            entity.setId(domain.getId());
        }
        entity.setActive(domain.isActive());

        if (domain.getStudentId() != null) {
            StudentEntity studentEntity = new StudentEntity();
            studentEntity.setId(domain.getStudentId());
            entity.setStudentEntity(studentEntity);
        }

        if (domain.getTimeBadge() != null) {
            ScheduleEntity franjaEntity = new ScheduleEntity();
            franjaEntity.setId(domain.getTimeBadge().getId());
            entity.setTimeSlotEntity(franjaEntity);
        }

        if (domain.getCategory() != null) {
            ProductCategoryEntity catEntity = new ProductCategoryEntity();
            catEntity.setId(domain.getCategory().getId());
            entity.setCategory(catEntity);
        } else {
            entity.setCategory(null);
        }

        if (domain.getClassification() != null) {
            TypeClassificationEntity tipoEntity = new TypeClassificationEntity();
            tipoEntity.setId(domain.getClassification().getId());
            entity.setTypeClassificationEntity(tipoEntity);
        } else {
            entity.setTypeClassificationEntity(null);
        }

        if (entity.getCreatedByUserEntity() == null) {
            UserEntity usuarioDummy = new UserEntity();
            usuarioDummy.setId(UUID.randomUUID());
            entity.setCreatedByUserEntity(usuarioDummy);
        }
    }
}