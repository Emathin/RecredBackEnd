package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.infrastructure.entities.KiosqueroEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

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

    @Override
    public UUID findBuffetByUsuarioId(UUID usuarioId) {
        return springDataKiosqueroRepository.findBuffetByUsuarioId(usuarioId);
    }

    public Optional<UUID> findKiosqueroIdByUsuarioId(UUID usuarioId) {
        return springDataKiosqueroRepository.findKiosqueroIdByUsuarioId(usuarioId);
    }

    @Override
    public Optional<UUID> findSchoolIdByUsuarioId(UUID usuarioId) {
        return springDataKiosqueroRepository.findSchoolIdByUsuarioId(usuarioId);
    }

    @Override
    public UUID findUsuarioIdById(UUID kiosqueroId) {
        return springDataKiosqueroRepository.findUsuarioId(kiosqueroId);
    }

    @Override
    public List<UUID> findAllIds() {
        return springDataKiosqueroRepository.findAllIds();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Kiosquero> findAll() {
        List<Kiosquero> kiosqueros = springDataKiosqueroRepository.findAll()
                .stream().map(KiosqueroEntity::toDomain).collect(toList());

        return kiosqueros;
    }

    @Override
    @Transactional
    public Kiosquero save(Kiosquero kiosquero) {
        KiosqueroEntity entity = KiosqueroEntity.fromDomain(kiosquero);
        KiosqueroEntity savedEntity = springDataKiosqueroRepository.save(entity);
        return savedEntity.toDomain();
    }
}
