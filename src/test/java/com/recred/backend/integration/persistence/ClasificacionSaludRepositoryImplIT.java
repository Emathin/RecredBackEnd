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
        ClasificacionSaludEntity entity1 = generateCeliac();
        ClasificacionSaludEntity entity2 = generateDiabetic();

        List<java.util.UUID> searchIds = List.of(entity1.getId(), entity2.getId());

        List<ClasificacionSalud> result = repositoryImpl.findAllById(searchIds);

        assertNotNull(result);
        assertEquals(2, result.size());

        assertTrue(result.stream().anyMatch(c -> c.getId().equals(entity1.getId()) && "Celiac".equals(c.getDescripcion())));
        assertTrue(result.stream().anyMatch(c -> c.getId().equals(entity2.getId()) && "Diabetic".equals(c.getDescripcion())));
    }

    @Test
    void findById_ShouldMapAndReturn_WhenExists() {
        ClasificacionSaludEntity entity = generateLactoseIntolerant();

        Optional<ClasificacionSalud> result = repositoryImpl.findById(entity.getId());

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
        assertEquals("Lactose Intolerant", result.get().getDescripcion());
    }

    @Test
    void findById_ShouldReturnEmpty_WhenDoesNotExist() {
        Optional<ClasificacionSalud> result = repositoryImpl.findById(java.util.UUID.randomUUID());

        assertTrue(result.isEmpty());
    }

    @Test
    void findAll_ShouldMapAllEntitiesToDomain() {
        generateCeliac();

        List<ClasificacionSalud> result = repositoryImpl.findAll();

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(c -> "Celiac".equals(c.getDescripcion())));
    }

    @Test
    void findAll_ShouldReturnEmptyList_WhenNoDataExists() {
        springDataRepository.deleteAll();

        List<ClasificacionSalud> result = repositoryImpl.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void toEntity_ShouldMapModelToEntity() {
        ClasificacionSalud model = new ClasificacionSalud();
        model.setId(java.util.UUID.randomUUID());
        model.setDescripcion("Sugar Free");
        model.setActivo(true);

        ClasificacionSaludEntity entity = ReflectionTestUtils.invokeMethod(
                repositoryImpl,
                "toEntity",
                model
        );

        assertNotNull(entity);
        assertEquals(model.getId(), entity.getId());
        assertEquals("Sugar Free", entity.getDescripcion());
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
