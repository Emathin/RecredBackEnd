package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.infrastructure.entities.TutorEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TutorRepositoryImpl implements TutorRepository {

    private final SpringDataTutorRepository springDataTutorRepository;

    public TutorRepositoryImpl(SpringDataTutorRepository springDataTutorRepository) {
        this.springDataTutorRepository = springDataTutorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tutor> findById(UUID id) {
        return springDataTutorRepository.findById(id)
                .map(TutorEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tutor> findByUsuarioIdCognito(String cognitoSubject) {
        return springDataTutorRepository.findByUsuarioIdCognito(cognitoSubject)
                .map(TutorEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tutor> findByUsuarioId(UUID userId) {
        return springDataTutorRepository.findByUsuarioId(userId)
                .map(TutorEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tutor> findAll() {
        return springDataTutorRepository.findAll()
                .stream()
                .map(TutorEntity::toDomain)
                .toList();
    }
}
