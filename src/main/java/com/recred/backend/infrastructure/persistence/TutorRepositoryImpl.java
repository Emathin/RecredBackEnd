package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.infrastructure.entities.TutorEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class TutorRepositoryImpl implements TutorRepository {

    private final SpringDataTutorRepository springDataTutorRepository;

    public TutorRepositoryImpl(SpringDataTutorRepository springDataTutorRepository) {
        this.springDataTutorRepository = springDataTutorRepository;
    }

    @Override
    public Optional<Tutor> findById(UUID id) {
        return springDataTutorRepository.findById(id)
                .map(TutorEntity::toDomain);
    }
}
