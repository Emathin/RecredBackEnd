package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ColegioRepositoryImpl implements ColegioRepository {

    private final SpringDataColegioRepository springDataColegioRepository;

    public ColegioRepositoryImpl(SpringDataColegioRepository springDataColegioRepository) {
        this.springDataColegioRepository = springDataColegioRepository;
    }

    @Override
    public List<Colegio> findAllActive() {
        return springDataColegioRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(ColegioEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsActiveById(UUID id) {
        return springDataColegioRepository.existsByIdAndActivoTrue(id);
    }

    @Override
    public Colegio save(Colegio colegio) {
        ColegioEntity entity = ColegioEntity.fromDomain(colegio);
        ColegioEntity savedEntity = springDataColegioRepository.save(entity);
        return savedEntity.toDomain();
    }
}
