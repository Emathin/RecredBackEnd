package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Merchant;
import com.recred.backend.domain.repositories.MerchantRepository;
import com.recred.backend.infrastructure.entities.MerchantEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Component
public class MerchantRepositoryImpl implements MerchantRepository {

    private final SpringDataKiosqueroRepository springDataKiosqueroRepository;

    public MerchantRepositoryImpl(SpringDataKiosqueroRepository springDataKiosqueroRepository){
        this.springDataKiosqueroRepository = springDataKiosqueroRepository;
    }

    @Override
    public UUID findBuffetById(UUID kiosqueroId) {
        return springDataKiosqueroRepository.findBuffet(kiosqueroId);
    }

    @Override
    public List<Merchant> findAll() {
        List<Merchant> merchants = springDataKiosqueroRepository.findAll()
                .stream().map(MerchantEntity::toDomain).collect(toList());

        return merchants;
    }
}
