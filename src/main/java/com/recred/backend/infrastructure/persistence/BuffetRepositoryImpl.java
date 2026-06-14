package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class BuffetRepositoryImpl implements BuffetRepository {

    private final SpringDataBuffetRepository springDataBuffetRepository;

    public BuffetRepositoryImpl(SpringDataBuffetRepository springDataBuffetRepository) {
        this.springDataBuffetRepository = springDataBuffetRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Buffet> findById(UUID id) {
        return springDataBuffetRepository.findById(id).map(BuffetEntity::toDomain);
    }
}
