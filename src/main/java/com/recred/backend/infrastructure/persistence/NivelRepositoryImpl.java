package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Nivel;
import com.recred.backend.domain.repositories.NivelRepository;
import com.recred.backend.infrastructure.entities.NivelEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class NivelRepositoryImpl implements NivelRepository {

    private final SpringDataNivelRepository springDataNivelRepository;

    public NivelRepositoryImpl(SpringDataNivelRepository springDataNivelRepository) {
        this.springDataNivelRepository = springDataNivelRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Nivel> findAllActive() {
        return springDataNivelRepository.findByActivoTrueOrderByDescripcionAsc()
                .stream()
                .map(NivelEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Nivel> findActiveById(UUID id) {
        return springDataNivelRepository.findByIdAndActivoTrue(id)
                .map(NivelEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Nivel> findByDescripcionIgnoreCase(String descripcion) {
        return springDataNivelRepository.findByDescripcionIgnoreCase(descripcion)
                .map(NivelEntity::toDomain);
    }

    @Override
    @Transactional
    public Nivel save(Nivel nivel) {
        NivelEntity saved = springDataNivelRepository.save(NivelEntity.fromDomain(nivel));
        return saved.toDomain();
    }
}
