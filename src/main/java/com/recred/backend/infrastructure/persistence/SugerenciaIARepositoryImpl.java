package com.recred.backend.infrastructure.persistence;


import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class SugerenciaIARepositoryImpl implements SugerenciaIARepository {

    private final SpringDataSugerenciaIARepository springDataRepository;

    public SugerenciaIARepositoryImpl(SpringDataSugerenciaIARepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public void guardar(SugerenciaIAEntity sugerencia) {
        springDataRepository.save(sugerencia);
    }

    @Override
    public List<SugerenciaIAEntity> findByAlumnoId(UUID alumnoId) {
        return springDataRepository.findAllByAlumnoEntityIdOrderByCreadoEnDesc(alumnoId);
    }

    @Override
    public java.util.Optional<SugerenciaIAEntity> findById(UUID id) {
        return springDataRepository.findById(id);
    }
}
