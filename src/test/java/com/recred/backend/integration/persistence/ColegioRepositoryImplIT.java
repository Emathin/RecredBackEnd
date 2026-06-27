package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.Colegio;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.persistence.ColegioRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataColegioRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class ColegioRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private SpringDataColegioRepository springDataColegioRepository;

    @Autowired
    private ColegioRepositoryImpl repository;

    private ColegioEntity cachedNorthSchool;
    private ColegioEntity cachedSouthSchool;

    @Test
    void findAllActive_ShouldMapActiveSchoolsToDomain() {
        ColegioEntity first = generateNorthSchool();
        ColegioEntity second = generateSouthSchool();

        List<Colegio> result = repository.findAllActive();

        assertTrue(result.size() >= 2);
        assertTrue(result.stream().anyMatch(c -> c.getId().equals(first.getId()) && "North School".equals(c.getNombre())));
        assertTrue(result.stream().anyMatch(c -> c.getId().equals(second.getId()) && "South School".equals(c.getNombre())));
    }

    @Test
    void existsActiveById_ShouldDelegateToSpringDataRepository() {
        ColegioEntity school = generateNorthSchool();

        assertTrue(repository.existsActiveById(school.getId()));
    }

    private ColegioEntity generateNorthSchool() {
        if (this.cachedNorthSchool == null) {
            ColegioEntity entity = new ColegioEntity();
            entity.setNombre("North School");
            entity.setCue("123456");
            entity.setActivo(true);
            this.cachedNorthSchool = springDataColegioRepository.saveAndFlush(entity);
        }
        return this.cachedNorthSchool;
    }

    private ColegioEntity generateSouthSchool() {
        if (this.cachedSouthSchool == null) {
            ColegioEntity entity = new ColegioEntity();
            entity.setNombre("South School");
            entity.setCue("1234568");
            entity.setActivo(true);
            this.cachedSouthSchool = springDataColegioRepository.saveAndFlush(entity);
        }
        return this.cachedSouthSchool;
    }
}
