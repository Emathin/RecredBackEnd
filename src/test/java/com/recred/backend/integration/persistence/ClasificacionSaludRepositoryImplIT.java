package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.infrastructure.entities.ClasificacionSaludEntity;
import com.recred.backend.infrastructure.persistence.ClasificacionSaludRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataClasificacionSaludRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class ClasificacionSaludRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private SpringDataClasificacionSaludRepository springDataRepository;

    @Autowired
    private ClasificacionSaludRepositoryImpl repositoryImpl;

    private ClasificacionSaludEntity cachedCeliac;
    private ClasificacionSaludEntity cachedDiabetic;
    private ClasificacionSaludEntity cachedLactoseIntolerant;

    @Test
    void findAllById_ShouldMapEntitiesToDomainAndReturn() {
        ClasificacionSaludEntity entity1 = givenCeliacEntity();
        ClasificacionSaludEntity entity2 = givenDiabeticEntity();
        List<UUID> searchIds = List.of(entity1.getId(), entity2.getId());

        List<ClasificacionSalud> result = whenFindingAllById(searchIds);

        thenEntitiesAreMappedAndReturned(result, entity1, entity2);
    }

    @Test
    void findById_ShouldMapAndReturn_WhenExists() {
        ClasificacionSaludEntity entity = givenLactoseIntolerantEntity();

        Optional<ClasificacionSalud> result = whenFindingById(entity.getId());

        thenEntityIsMappedAndReturned(result, entity);
    }

    @Test
    void findById_ShouldReturnEmpty_WhenDoesNotExist() {
        Optional<ClasificacionSalud> result = whenFindingById(UUID.randomUUID());

        thenResultIsEmpty(result);
    }

    @Test
    void findAll_ShouldMapAllEntitiesToDomain() {
        givenCeliacEntity();

        List<ClasificacionSalud> result = whenFindingAll();

        thenAllEntitiesAreMapped(result);
    }

    @Test
    void findAll_ShouldReturnEmptyList_WhenNoDataExists() {
        givenNoDataExists();

        List<ClasificacionSalud> result = whenFindingAll();

        thenResultListIsEmpty(result);
    }

    @Test
    void toEntity_ShouldMapModelToEntity() {
        ClasificacionSalud model = givenHealthClassificationModel("Sugar Free");

        ClasificacionSaludEntity entity = whenMappingToEntity(model);

        thenModelIsMappedCorrectly(entity, model);
    }

    private ClasificacionSaludEntity givenCeliacEntity() {
        return generateCeliac();
    }

    private ClasificacionSaludEntity givenDiabeticEntity() {
        return generateDiabetic();
    }

    private ClasificacionSaludEntity givenLactoseIntolerantEntity() {
        return generateLactoseIntolerant();
    }

    private void givenNoDataExists() {
        springDataRepository.deleteAll();
    }

    private ClasificacionSalud givenHealthClassificationModel(String description) {
        ClasificacionSalud model = new ClasificacionSalud();
        model.setId(UUID.randomUUID());
        model.setDescripcion(description);
        model.setActivo(true);
        return model;
    }

    private List<ClasificacionSalud> whenFindingAllById(List<UUID> searchIds) {
        return repositoryImpl.findAllById(searchIds);
    }

    private Optional<ClasificacionSalud> whenFindingById(UUID id) {
        return repositoryImpl.findById(id);
    }

    private List<ClasificacionSalud> whenFindingAll() {
        return repositoryImpl.findAll();
    }

    private ClasificacionSaludEntity whenMappingToEntity(ClasificacionSalud model) {
        return ReflectionTestUtils.invokeMethod(repositoryImpl, "toEntity", model);
    }

    private void thenEntitiesAreMappedAndReturned(List<ClasificacionSalud> result, ClasificacionSaludEntity entity1, ClasificacionSaludEntity entity2) {
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(c -> c.getId().equals(entity1.getId()) && "Celiac".equals(c.getDescripcion())));
        assertTrue(result.stream().anyMatch(c -> c.getId().equals(entity2.getId()) && "Diabetic".equals(c.getDescripcion())));
    }

    private void thenEntityIsMappedAndReturned(Optional<ClasificacionSalud> result, ClasificacionSaludEntity entity) {
        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
        assertEquals("Lactose Intolerant", result.get().getDescripcion());
    }

    private void thenResultIsEmpty(Optional<ClasificacionSalud> result) {
        assertTrue(result.isEmpty());
    }

    private void thenAllEntitiesAreMapped(List<ClasificacionSalud> result) {
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(c -> "Celiac".equals(c.getDescripcion())));
    }

    private void thenResultListIsEmpty(List<ClasificacionSalud> result) {
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    private void thenModelIsMappedCorrectly(ClasificacionSaludEntity entity, ClasificacionSalud model) {
        assertNotNull(entity);
        assertEquals(model.getId(), entity.getId());
        assertEquals(model.getDescripcion(), entity.getDescripcion());
        assertTrue(entity.getActivo());
    }

    private ClasificacionSaludEntity generateCeliac() {
        if (this.cachedCeliac == null) {
            ClasificacionSaludEntity entity = new ClasificacionSaludEntity();
            entity.setDescripcion("Celiac");
            entity.setActivo(true);
            this.cachedCeliac = springDataRepository.saveAndFlush(entity);
        }
        return this.cachedCeliac;
    }

    private ClasificacionSaludEntity generateDiabetic() {
        if (this.cachedDiabetic == null) {
            ClasificacionSaludEntity entity = new ClasificacionSaludEntity();
            entity.setDescripcion("Diabetic");
            entity.setActivo(true);
            this.cachedDiabetic = springDataRepository.saveAndFlush(entity);
        }
        return this.cachedDiabetic;
    }

    private ClasificacionSaludEntity generateLactoseIntolerant() {
        if (this.cachedLactoseIntolerant == null) {
            ClasificacionSaludEntity entity = new ClasificacionSaludEntity();
            entity.setDescripcion("Lactose Intolerant");
            entity.setActivo(true);
            this.cachedLactoseIntolerant = springDataRepository.saveAndFlush(entity);
        }
        return this.cachedLactoseIntolerant;
    }
}
