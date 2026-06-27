package com.recred.backend.integration.persistence;

import com.recred.backend.integration.AbstractIntegrationTest;

import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.FranjaHorariaEntity;
import com.recred.backend.infrastructure.persistence.FranjaHorariaRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataFranjaHorariaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class FranjaHorariaRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private FranjaHorariaRepositoryImpl repository;

    @Autowired
    private SpringDataFranjaHorariaRepository springDataRepository;

    @Autowired
    private EntityManager entityManager;

    private ColegioEntity cachedSchool;

    @Test
    void save_ShouldCreateEntityFromDomainModel() {
        ColegioEntity school = generateSchoolEntity();
        FranjaHoraria domain = createTimeSlotDomain(null, school.getId());

        FranjaHoraria result = repository.save(domain);
        entityManager.flush();
        entityManager.clear();

        assertNotNull(result.getId());
        Optional<FranjaHorariaEntity> savedEntity = springDataRepository.findById(result.getId());
        assertTrue(savedEntity.isPresent());
        assertEquals("First break", savedEntity.get().getDescripcion());
        assertEquals(school.getId(), savedEntity.get().getColegioEntity().getId());
    }

    @Test
    void save_ShouldUpdateExistingEntity() {
        ColegioEntity school = generateSchoolEntity();
        FranjaHorariaEntity existing = generateTimeSlotEntity(school);
        FranjaHoraria domain = createTimeSlotDomain(existing.getId(), school.getId());
        domain.setDescripcion("Updated break");

        FranjaHoraria result = repository.save(domain);
        entityManager.flush();
        entityManager.clear();

        Optional<FranjaHorariaEntity> updatedEntity = springDataRepository.findById(existing.getId());
        assertTrue(updatedEntity.isPresent());
        assertEquals("Updated break", updatedEntity.get().getDescripcion());
        assertEquals(existing.getId(), result.getId());
    }

    @Test
    void findById_ShouldMapEntityWhenPresent() {
        ColegioEntity school = generateSchoolEntity();
        FranjaHorariaEntity entity = generateTimeSlotEntity(school);

        Optional<FranjaHoraria> result = repository.findById(entity.getId());

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
        assertEquals(school.getId(), result.get().getColegioId());
    }

    @Test
    void findAllByColegioId_ShouldMapEntities() {
        ColegioEntity school = generateSchoolEntity();
        generateTimeSlotEntity(school);
        generateTimeSlotEntity(school);

        List<FranjaHoraria> result = repository.findAllByColegioId(school.getId());

        assertEquals(2, result.size());
    }

    private ColegioEntity generateSchoolEntity() {
        if (this.cachedSchool == null) {
            ColegioEntity school = new ColegioEntity();
            school.setNombre("Time Slot School");
            school.setCue("CUE-TIME-SLOT");
            school.setActivo(true);
            entityManager.persist(school);
            this.cachedSchool = school;
        }
        return this.cachedSchool;
    }

    private FranjaHorariaEntity generateTimeSlotEntity(ColegioEntity school) {
        FranjaHorariaEntity entity = new FranjaHorariaEntity();
        entity.setColegioEntity(school);
        entity.setDescripcion("Morning break");
        entity.setHoraInicio(LocalTime.of(9, 0));
        entity.setHoraFin(LocalTime.of(9, 15));
        entity.setActivo(true);
        entity.setCupoMaximo(20);
        entity.setMinutosCorte(5);
        return springDataRepository.save(entity);
    }

    private FranjaHoraria createTimeSlotDomain(UUID id, UUID schoolId) {
        FranjaHoraria timeSlot = new FranjaHoraria();
        timeSlot.setId(id);
        timeSlot.setColegioId(schoolId);
        timeSlot.setDescripcion("First break");
        timeSlot.setHoraInicio(LocalTime.of(10, 0));
        timeSlot.setHoraFin(LocalTime.of(10, 15));
        timeSlot.setActivo(true);
        timeSlot.setCupoMaximo(30);
        timeSlot.setMinutosCorte(-5);
        return timeSlot;
    }
}
