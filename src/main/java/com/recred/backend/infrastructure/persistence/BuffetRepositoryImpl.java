package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class BuffetRepositoryImpl implements BuffetRepository {

    private final SpringDataBuffetRepository springDataBuffetRepository;

    public BuffetRepositoryImpl(SpringDataBuffetRepository springDataBuffetRepository) {
        this.springDataBuffetRepository = springDataBuffetRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Buffet> findById(UUID id) {
        return springDataBuffetRepository.findByIdWithSchool(id).map(BuffetEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Buffet> findByAlumnoId(UUID alumnoId) {
        return springDataBuffetRepository.findByAlumnoId(alumnoId)
                .map(BuffetEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Buffet> findByColegioId(UUID colegioId) {
        return springDataBuffetRepository.findByColegioEntityId(colegioId).stream()
                .map(BuffetEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Buffet save(Buffet buffet) {
        BuffetEntity entity = BuffetEntity.fromDomain(buffet);
        BuffetEntity savedEntity = springDataBuffetRepository.save(entity);
        return savedEntity.toDomain();
    }
}

