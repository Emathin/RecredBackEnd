package com.recred.backend.infrastructure.persistence;


import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.springframework.stereotype.Component;

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
}
