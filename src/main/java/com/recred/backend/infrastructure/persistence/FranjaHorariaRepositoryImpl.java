package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
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
}