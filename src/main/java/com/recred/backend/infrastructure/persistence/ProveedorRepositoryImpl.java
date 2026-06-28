package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Proveedor;
import com.recred.backend.domain.repositories.ProveedorRepository;
import com.recred.backend.infrastructure.entities.ProveedorEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProveedorRepositoryImpl implements ProveedorRepository {

    private final SpringDataProveedorRepository springDataProveedorRepository;

    public ProveedorRepositoryImpl(SpringDataProveedorRepository springDataProveedorRepository) {
        this.springDataProveedorRepository = springDataProveedorRepository;
    }

    @Override
    @Transactional
    public Proveedor save(Proveedor proveedor) {
        ProveedorEntity entity = ProveedorEntity.fromDomain(proveedor);
        ProveedorEntity saved = springDataProveedorRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Proveedor> findById(UUID id) {
        return springDataProveedorRepository.findById(id)
                .map(ProveedorEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Proveedor> findByIdAndKiosqueroId(UUID id, UUID kiosqueroId) {
        return springDataProveedorRepository.findByIdAndKiosqueroId(id, kiosqueroId)
                .map(ProveedorEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Proveedor> findByKiosqueroId(UUID kiosqueroId) {
        return springDataProveedorRepository.findByKiosqueroId(kiosqueroId).stream()
                .map(ProveedorEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Proveedor proveedor) {
        springDataProveedorRepository.delete(ProveedorEntity.fromDomain(proveedor));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByIdAndKiosqueroId(UUID id, UUID kiosqueroId) {
        return springDataProveedorRepository.existsByIdAndKiosqueroId(id, kiosqueroId);
    }
}
