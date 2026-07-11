package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.Grado;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.GradoEntity;
import com.recred.backend.infrastructure.entities.NivelEntity;
import com.recred.backend.infrastructure.persistence.GradoRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataGradoRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

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
        GradoEntity grade = givenPersistedGrade();

        Optional<Grado> result = whenFindingById(grade.getId());

        thenGradeIsMappedCorrectly(result, grade);
    }

    @Test
    void findById_WhenGradeDoesNotExist_ShouldReturnEmptyOptional() {
        Optional<Grado> result = whenFindingById(UUID.randomUUID());

        assertTrue(result.isEmpty());
    }

    @Test
    void findAllActiveByColegioId_ShouldMapActiveGradesToDomain() {
        UUID schoolId = givenTwoActiveGradesForSchool();

        List<Grado> result = whenFindingAllActiveBySchoolId(schoolId);

        thenTwoGradesAreMapped(result);
    }

    @Test
    void findAllByColegioIdIncludingInactive_ShouldMapInactiveGradesToDomain() {
        UUID schoolId = givenActiveAndInactiveGradesForSchool();

        List<Grado> result = whenFindingAllGradesIncludingInactive(schoolId);

        thenActiveAndInactiveGradesAreMapped(result);
    }

    @Test
    void existsActiveByColegioIdAndNivelIdAndAnioAndDivision_ShouldIgnoreExcludedAndInactive() {
        GradoEntity active = givenActiveAndInactiveGradesForDuplicateCheck();
        UUID schoolId = active.getColegioEntity().getId();
        UUID levelId = active.getNivelEntity().getId();

        boolean existingActive = whenCheckingActiveDuplicateExists(schoolId, levelId);
        boolean excludedActive = whenCheckingExcludedActiveDuplicate(schoolId, levelId, active.getId());
        boolean inactiveDuplicate = whenCheckingInactiveDuplicateExists(schoolId, levelId);

        thenDuplicateLogicWorksCorrectly(existingActive, excludedActive, inactiveDuplicate);
    }

    @Test
    void save_ShouldPersistGradeForSchoolReference() {
        Grado grade = givenNewGradeDomain();

        Grado saved = whenSavingGrade(grade);

        thenGradeIsPersisted(saved, grade.getColegio().getId());
    }

    private GradoEntity givenPersistedGrade() {
        ColegioEntity school = generateSchoolEntity();
        return generateGradeEntity(school, "1", "A");
    }

    private UUID givenTwoActiveGradesForSchool() {
        ColegioEntity school = generateSchoolEntity();
        generateGradeEntity(school, "1", "A");
        generateGradeEntity(school, "2", "B");
        return school.getId();
    }

    private UUID givenActiveAndInactiveGradesForSchool() {
        ColegioEntity school = generateSchoolEntity();
        generateGradeEntity(school, "1", "A");
        givenInactiveGrade(school, "2", "B");
        return school.getId();
    }

    private GradoEntity givenActiveAndInactiveGradesForDuplicateCheck() {
        ColegioEntity school = generateSchoolEntity();
        generateLevelEntity();
        GradoEntity active = generateGradeEntity(school, "1", "A");
        givenInactiveGrade(school, "2", "B");
        return active;
    }

    private Grado givenNewGradeDomain() {
        ColegioEntity school = generateSchoolEntity();
        return domainGrade(school, "3", "C");
    }

    private Optional<Grado> whenFindingById(UUID id) {
        return repository.findById(id);
    }

    private List<Grado> whenFindingAllActiveBySchoolId(UUID schoolId) {
        return repository.findAllActiveByColegioId(schoolId);
    }

    private List<Grado> whenFindingAllGradesIncludingInactive(UUID schoolId) {
        return repository.findAllByColegioIdIncludingInactive(schoolId);
    }

    private boolean whenCheckingActiveDuplicateExists(UUID schoolId, UUID levelId) {
        return repository.existsActiveByColegioIdAndNivelIdAndAnioAndDivision(schoolId, levelId, null, "1", "A");
    }

    private boolean whenCheckingExcludedActiveDuplicate(UUID schoolId, UUID levelId, UUID excludedId) {
        return repository.existsActiveByColegioIdAndNivelIdAndAnioAndDivision(schoolId, levelId, excludedId, "1", "A");
    }

    private boolean whenCheckingInactiveDuplicateExists(UUID schoolId, UUID levelId) {
        return repository.existsActiveByColegioIdAndNivelIdAndAnioAndDivision(schoolId, levelId, null, "2", "B");
    }

    private Grado whenSavingGrade(Grado grade) {
        return repository.save(grade);
    }

    private void thenGradeIsMappedCorrectly(Optional<Grado> result, GradoEntity grade) {
        assertTrue(result.isPresent());
        assertEquals(grade.getId(), result.get().getId());
        assertEquals(grade.getAnio(), result.get().getAnio());
        assertEquals(grade.getDivision(), result.get().getDivision());
    }

    private void thenTwoGradesAreMapped(List<Grado> result) {
        assertEquals(2, result.size());
        assertEquals("1", result.get(0).getAnio());
        assertEquals("2", result.get(1).getAnio());
    }

    private void thenActiveAndInactiveGradesAreMapped(List<Grado> result) {
        assertEquals(2, result.size());
        assertTrue(result.get(0).getActivo());
        assertFalse(result.get(1).getActivo());
    }

    private void thenDuplicateLogicWorksCorrectly(boolean existingActive, boolean excludedActive, boolean inactiveDuplicate) {
        assertTrue(existingActive);
        assertFalse(excludedActive);
        assertFalse(inactiveDuplicate);
    }

    private void thenGradeIsPersisted(Grado saved, UUID expectedSchoolId) {
        assertTrue(springDataGradoRepository.findById(saved.getId()).isPresent());
        assertEquals(expectedSchoolId, saved.getColegio().getId());
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

    private GradoEntity givenInactiveGrade(ColegioEntity school, String year, String division) {
        GradoEntity inactive = generateGradeEntity(school, year, division);
        inactive.setActivo(false);
        return springDataGradoRepository.saveAndFlush(inactive);
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
