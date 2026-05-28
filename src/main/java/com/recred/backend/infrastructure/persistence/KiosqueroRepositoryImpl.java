package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.repositories.KiosqueroRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KiosqueroRepositoryImpl implements KiosqueroRepository {

    private final SpringDataKiosqueroRepository springDataKiosqueroRepository;

    public KiosqueroRepositoryImpl(SpringDataKiosqueroRepository springDataKiosqueroRepository){
        this.springDataKiosqueroRepository = springDataKiosqueroRepository;
    }

    @Override
    public UUID findBuffetById(UUID kiosqueroId) {
        return springDataKiosqueroRepository.findBuffet(kiosqueroId);
    }
}
