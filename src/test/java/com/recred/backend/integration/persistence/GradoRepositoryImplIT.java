package com.recred.backend.integration.persistence;

import com.recred.backend.integration.AbstractIntegrationTest;

import com.recred.backend.domain.models.Grado;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.GradoEntity;
import com.recred.backend.infrastructure.entities.NivelEntity;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private NivelEntity cachedLevel;

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

    @Test
    void findAllByColegioIdIncludingInactive_ShouldMapInactiveGradesToDomain() {
        ColegioEntity school = generateSchoolEntity();
        generateGradeEntity(school, "1", "A");
        givenInactiveGrade(school, "2", "B");

        List<Grado> result = whenFindingAllGradesIncludingInactive(school.getId());

        assertEquals(2, result.size());
        assertTrue(result.get(0).getActivo());
        assertFalse(result.get(1).getActivo());
    }

    @Test
    void existsActiveByColegioIdAndNivelIdAndAnioAndDivision_ShouldIgnoreExcludedAndInactive() {
        ColegioEntity school = generateSchoolEntity();
        NivelEntity level = generateLevelEntity();
        GradoEntity active = generateGradeEntity(school, "1", "A");
        givenInactiveGrade(school, "2", "B");

        boolean existingActive = whenCheckingActiveDuplicate(
                school.getId(), level.getId(), null, "1", "A");
        boolean excludedActive = whenCheckingActiveDuplicate(
                school.getId(), level.getId(), active.getId(), "1", "A");
        boolean inactiveDuplicate = whenCheckingActiveDuplicate(
                school.getId(), level.getId(), null, "2", "B");

        assertTrue(existingActive);
        assertFalse(excludedActive);
        assertFalse(inactiveDuplicate);
    }

    @Test
    void save_ShouldPersistGradeForSchoolReference() {
        ColegioEntity school = generateSchoolEntity();
        Grado grade = domainGrade(school, "3", "C");

        Grado saved = whenSavingGrade(grade);

        assertTrue(springDataGradoRepository.findById(saved.getId()).isPresent());
        assertEquals(school.getId(), saved.getColegio().getId());
        assertEquals("3", saved.getAnio());
        assertEquals("C", saved.getDivision());
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

    private NivelEntity generateLevelEntity() {
        if (this.cachedLevel == null) {
            this.cachedLevel = entityManager
                    .createQuery(
                            "select level from NivelEntity level "
                                    + "where level.descripcion = :descripcion",
                            NivelEntity.class)
                    .setParameter("descripcion", "Primario")
                    .getResultStream()
                    .findFirst()
                    .orElseGet(this::persistDefaultLevel);
        }
        return this.cachedLevel;
    }

    private NivelEntity persistDefaultLevel() {
        NivelEntity level = new NivelEntity();
        level.setDescripcion("Primario");
        level.setActivo(true);
        entityManager.persist(level);
        return level;
    }

    private GradoEntity generateGradeEntity(ColegioEntity school, String year, String division) {
        GradoEntity entity = new GradoEntity();
        entity.setColegioEntity(school);
        entity.setNivelEntity(generateLevelEntity());
        entity.setAnio(year);
        entity.setDivision(division);
        entity.setActivo(true);
        return springDataGradoRepository.save(entity);
    }

    private GradoEntity givenInactiveGrade(
            ColegioEntity school,
            String year,
            String division
    ) {
        GradoEntity inactive = generateGradeEntity(school, year, division);
        inactive.setActivo(false);
        return springDataGradoRepository.saveAndFlush(inactive);
    }

    private List<Grado> whenFindingAllGradesIncludingInactive(UUID schoolId) {
        return repository.findAllByColegioIdIncludingInactive(schoolId);
    }

    private boolean whenCheckingActiveDuplicate(
            UUID schoolId,
            UUID levelId,
            UUID excludedId,
            String year,
            String division
    ) {
        return repository.existsActiveByColegioIdAndNivelIdAndAnioAndDivision(
                schoolId,
                levelId,
                excludedId,
                year,
                division);
    }

    private Grado whenSavingGrade(Grado grade) {
        return repository.save(grade);
    }

    private Grado domainGrade(ColegioEntity school, String year, String division) {
        Grado grade = new Grado();
        grade.setColegio(school.toDomain());
        grade.setNivel(generateLevelEntity().toDomain());
        grade.setAnio(year);
        grade.setDivision(division);
        grade.setActivo(true);
        return grade;
    }
}
