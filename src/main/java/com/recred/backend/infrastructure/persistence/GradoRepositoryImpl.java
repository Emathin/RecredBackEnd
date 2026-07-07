package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.repositories.GradoRepository;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.GradoEntity;
import com.recred.backend.infrastructure.entities.NivelEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class GradoRepositoryImpl implements GradoRepository {

    private final SpringDataGradoRepository springDataGradoRepository;

    public GradoRepositoryImpl(SpringDataGradoRepository springDataGradoRepository) {
        this.springDataGradoRepository = springDataGradoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Grado> findById(UUID id) {
        return springDataGradoRepository.findById(id)
                .map(GradoEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Grado> findByIdAndColegioId(UUID id, UUID colegioId) {
        return springDataGradoRepository.findByIdAndColegioEntityId(id, colegioId)
                .map(GradoEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Grado> findAllActiveByColegioId(UUID colegioId) {
        return springDataGradoRepository
                .findByColegioEntityIdAndActivoTrueOrderByAnioAscDivisionAsc(colegioId)
                .stream()
                .map(GradoEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Grado> findAllByColegioIdIncludingInactive(UUID colegioId) {
        return springDataGradoRepository
                .findByColegioEntityIdOrderByActivoDescAnioAscDivisionAsc(colegioId)
                .stream()
                .map(GradoEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveByColegioIdAndNivelIdAndAnioAndDivision(
            UUID colegioId,
            UUID nivelId,
            UUID excludedId,
            String anio,
            String division
    ) {
        return springDataGradoRepository.existsActiveBySchoolAndValues(
                colegioId,
                nivelId,
                excludedId,
                anio,
                division);
    }

    @Override
    @Transactional
    public Grado save(Grado grado) {
        GradoEntity entity = existingEntity(grado);
        GradoEntity saved = springDataGradoRepository.save(toEntity(grado, entity));
        return saved.toDomain();
    }

    private GradoEntity existingEntity(Grado grado) {
        if (grado.getId() == null) {
            return new GradoEntity();
        }

        return springDataGradoRepository.findById(grado.getId())
                .orElse(new GradoEntity());
    }

    private GradoEntity toEntity(Grado grado, GradoEntity entity) {
        if (entity.getId() == null) {
            entity.setId(grado.getId());
        }

        entity.setAnio(grado.getAnio());
        entity.setDivision(grado.getDivision());
        entity.setActivo(grado.getActivo() != null ? grado.getActivo() : true);

        if (grado.getColegio() != null && grado.getColegio().getId() != null) {
            ColegioEntity school = new ColegioEntity();
            school.setId(grado.getColegio().getId());
            entity.setColegioEntity(school);
        }

        if (grado.getNivel() != null && grado.getNivel().getId() != null) {
            NivelEntity level = new NivelEntity();
            level.setId(grado.getNivel().getId());
            entity.setNivelEntity(level);
        }

        return entity;
    }
}
