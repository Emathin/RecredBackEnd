package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.FranjaHorariaEntity;
import com.recred.backend.infrastructure.persistence.FranjaHorariaRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataFranjaHorariaRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class FranjaHorariaRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private FranjaHorariaRepositoryImpl repository;

    @Autowired
    private SpringDataFranjaHorariaRepository springDataRepository;

    @Autowired
    private EntityManager entityManager;

    private ColegioEntity cachedSchool;

    @Test
    void save_ShouldCreateEntityFromDomainModel() {
        FranjaHoraria domain = givenNewTimeSlotDomain();

        FranjaHoraria result = whenSavingTimeSlot(domain);

        thenTimeSlotIsPersisted(result, domain.getColegioId());
    }

    @Test
    void save_ShouldUpdateExistingEntity() {
        FranjaHoraria domain = givenExistingTimeSlotDomainWithUpdate("Updated break");

        FranjaHoraria result = whenSavingTimeSlot(domain);

        thenTimeSlotIsUpdated(result);
    }

    @Test
    void findById_ShouldMapEntityWhenPresent() {
        FranjaHorariaEntity entity = givenPersistedTimeSlotEntity();

        Optional<FranjaHoraria> result = whenFindingById(entity.getId());

        thenEntityIsMappedCorrectly(result, entity);
    }

    @Test
    void findAllByColegioId_ShouldMapEntities() {
        UUID schoolId = givenTwoPersistedTimeSlotsForSameSchool();

        List<FranjaHoraria> result = whenFindingAllBySchoolId(schoolId);

        thenTwoTimeSlotsAreReturned(result);
    }

    @Test
    void findAllByColegioIdIncludingInactive_ShouldReturnInactiveEntities() {
        UUID schoolId = givenActiveAndInactiveTimeSlotsForSchool();

        List<FranjaHoraria> result = whenFindingAllIncludingInactive(schoolId);

        thenActiveAndInactiveTimeSlotsAreReturned(result);
    }

    @Test
    void existsActiveOverlap_ShouldIgnoreInactiveAndExcludedSlots() {
        FranjaHorariaEntity activeSlot = givenActiveAndInactiveTimeSlotsForSchoolOverlap();
        UUID schoolId = activeSlot.getColegioEntity().getId();

        boolean overlap = whenCheckingOverlap(schoolId, null);
        boolean excludedOverlap = whenCheckingOverlap(schoolId, activeSlot.getId());

        thenOverlapLogicWorksCorrectly(overlap, excludedOverlap);
    }

    private FranjaHoraria givenNewTimeSlotDomain() {
        ColegioEntity school = generateSchoolEntity();
        return createTimeSlotDomain(null, school.getId());
    }

    private FranjaHoraria givenExistingTimeSlotDomainWithUpdate(String newDescription) {
        ColegioEntity school = generateSchoolEntity();
        FranjaHorariaEntity existing = generateTimeSlotEntity(school);
        FranjaHoraria domain = createTimeSlotDomain(existing.getId(), school.getId());
        domain.setDescripcion(newDescription);
        return domain;
    }

    private FranjaHorariaEntity givenPersistedTimeSlotEntity() {
        ColegioEntity school = generateSchoolEntity();
        return generateTimeSlotEntity(school);
    }

    private UUID givenTwoPersistedTimeSlotsForSameSchool() {
        ColegioEntity school = generateSchoolEntity();
        generateTimeSlotEntity(school);
        generateTimeSlotEntity(school);
        return school.getId();
    }

    private UUID givenActiveAndInactiveTimeSlotsForSchool() {
        ColegioEntity school = generateSchoolEntity();
        generateTimeSlotEntity(school, true);
        generateTimeSlotEntity(school, false);
        return school.getId();
    }

    private FranjaHorariaEntity givenActiveAndInactiveTimeSlotsForSchoolOverlap() {
        ColegioEntity school = generateSchoolEntity();
        FranjaHorariaEntity activeSlot = generateTimeSlotEntity(school, true);
        generateTimeSlotEntity(school, false);
        return activeSlot;
    }

    private FranjaHoraria whenSavingTimeSlot(FranjaHoraria domain) {
        FranjaHoraria result = repository.save(domain);
        entityManager.flush();
        entityManager.clear();
        return result;
    }

    private Optional<FranjaHoraria> whenFindingById(UUID id) {
        return repository.findById(id);
    }

    private List<FranjaHoraria> whenFindingAllBySchoolId(UUID schoolId) {
        return repository.findAllByColegioId(schoolId);
    }

    private List<FranjaHoraria> whenFindingAllIncludingInactive(UUID schoolId) {
        return repository.findAllByColegioIdIncludingInactive(schoolId);
    }

    private boolean whenCheckingOverlap(UUID schoolId, UUID excludedId) {
        return repository.existsActiveOverlap(schoolId, excludedId, LocalTime.of(9, 5), LocalTime.of(9, 10));
    }

    private void thenTimeSlotIsPersisted(FranjaHoraria result, UUID schoolId) {
        assertNotNull(result.getId());
        Optional<FranjaHorariaEntity> savedEntity = springDataRepository.findById(result.getId());
        assertTrue(savedEntity.isPresent());
        assertEquals("First break", savedEntity.get().getDescripcion());
        assertEquals(schoolId, savedEntity.get().getColegioEntity().getId());
    }

    private void thenTimeSlotIsUpdated(FranjaHoraria result) {
        Optional<FranjaHorariaEntity> updatedEntity = springDataRepository.findById(result.getId());
        assertTrue(updatedEntity.isPresent());
        assertEquals("Updated break", updatedEntity.get().getDescripcion());
    }

    private void thenEntityIsMappedCorrectly(Optional<FranjaHoraria> result, FranjaHorariaEntity entity) {
        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
        assertEquals(entity.getColegioEntity().getId(), result.get().getColegioId());
    }

    private void thenTwoTimeSlotsAreReturned(List<FranjaHoraria> result) {
        assertEquals(2, result.size());
    }

    private void thenActiveAndInactiveTimeSlotsAreReturned(List<FranjaHoraria> result) {
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(slot -> !slot.isActivo()));
    }

    private void thenOverlapLogicWorksCorrectly(boolean overlap, boolean excludedOverlap) {
        assertTrue(overlap);
        assertFalse(excludedOverlap);
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
        return generateTimeSlotEntity(school, true);
    }

    private FranjaHorariaEntity generateTimeSlotEntity(ColegioEntity school, boolean active) {
        FranjaHorariaEntity entity = new FranjaHorariaEntity();
        entity.setColegioEntity(school);
        entity.setDescripcion("Morning break");
        entity.setHoraInicio(LocalTime.of(9, 0));
        entity.setHoraFin(LocalTime.of(9, 15));
        entity.setActivo(active);
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
