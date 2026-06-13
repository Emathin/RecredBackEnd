package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.FranjaHorariaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class FranjaHorariaRepositoryImpl implements FranjaHorariaRepository {

    private final SpringDataFranjaHorariaRepository springDataRepository;

    public FranjaHorariaRepositoryImpl(SpringDataFranjaHorariaRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public List<FranjaHoraria> findAllByColegioId(UUID colegioId) {
        return springDataRepository.findByColegioEntityIdAndActivoTrue(colegioId)
                .stream()
                .map(FranjaHorariaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public java.util.Optional<FranjaHoraria> findById(UUID id) {
        return springDataRepository.findById(id).map(FranjaHorariaEntity::toDomain);
    }

    @Override
    public FranjaHoraria save(FranjaHoraria franjaHoraria) {
        FranjaHorariaEntity entityExistente = null;

        if (franjaHoraria.getId() != null) {
            entityExistente = springDataRepository.findById(franjaHoraria.getId())
                    .orElse(new FranjaHorariaEntity());
        } else {
            entityExistente = new FranjaHorariaEntity();
        }

        FranjaHorariaEntity entityLista = toEntity(franjaHoraria, entityExistente);
        FranjaHorariaEntity guardada = springDataRepository.save(entityLista);

        return guardada.toDomain();
    }

    private FranjaHorariaEntity toEntity(FranjaHoraria modelo, FranjaHorariaEntity entity) {
        if (entity.getId() == null) {
            entity.setId(modelo.getId());
        }

        entity.setDescripcion(modelo.getDescripcion());
        entity.setHoraInicio(modelo.getHoraInicio());
        entity.setHoraFin(modelo.getHoraFin());
        entity.setActivo(modelo.isActivo());

        if (modelo.getColegioId() != null) {
            ColegioEntity colegio = new ColegioEntity();
            colegio.setId(modelo.getColegioId());
            entity.setColegioEntity(colegio);
        }

        return entity;
    }
}