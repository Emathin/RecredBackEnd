package com.recred.backend.integration.persistence;

import com.recred.backend.integration.AbstractIntegrationTest;

import com.recred.backend.domain.models.Grado;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.GradoEntity;
import com.recred.backend.infrastructure.persistence.GradoRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataGradoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class GradoRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private GradoRepositoryImpl repository;

    @Autowired
    private SpringDataGradoRepository springDataGradoRepository;

    @Autowired
    private EntityManager entityManager;

    private ColegioEntity cachedSchool;

    @Test
    void findById_WhenGradeExists_ShouldMapToDomain() {
        ColegioEntity school = generateSchoolEntity();
        GradoEntity grade = generateGradeEntity(school, "1", "A");

        Optional<Grado> result = repository.findById(grade.getId());

        assertTrue(result.isPresent());
        assertEquals(grade.getId(), result.get().getId());
        assertEquals("1", result.get().getAnio());
        assertEquals("A", result.get().getDivision());
    }

    @Test
    void findById_WhenGradeDoesNotExist_ShouldReturnEmptyOptional() {
        Optional<Grado> result = repository.findById(UUID.randomUUID());

        assertTrue(result.isEmpty());
    }

    @Test
    void findAllActiveByColegioId_ShouldMapActiveGradesToDomain() {
        ColegioEntity school = generateSchoolEntity();
        generateGradeEntity(school, "1", "A");
        generateGradeEntity(school, "2", "B");

        List<Grado> result = repository.findAllActiveByColegioId(school.getId());

        assertEquals(2, result.size());
        assertEquals("1", result.get(0).getAnio());
        assertEquals("2", result.get(1).getAnio());
    }

    private ColegioEntity generateSchoolEntity() {
        if (this.cachedSchool == null) {
            ColegioEntity school = new ColegioEntity();
            school.setNombre("Grade School");
            school.setCue("CUE-GRADE");
            school.setActivo(true);
            entityManager.persist(school);
            this.cachedSchool = school;
        }
        return this.cachedSchool;
    }

    private GradoEntity generateGradeEntity(ColegioEntity school, String year, String division) {
        GradoEntity entity = new GradoEntity();
        entity.setColegioEntity(school);
        entity.setAnio(year);
        entity.setDivision(division);
        entity.setActivo(true);
        return springDataGradoRepository.save(entity);
    }
}
