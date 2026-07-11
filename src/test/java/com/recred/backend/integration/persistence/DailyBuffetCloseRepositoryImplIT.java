package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.models.DailyInventorySnapshot;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.CierreDiarioBuffetEntity;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.DailyBuffetCloseRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataDailyBuffetCloseRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.recred.backend.domain.models.EstadoInventario.DISPONIBLE;
import static com.recred.backend.domain.models.RolUsuario.VENDEDOR;
import static com.recred.backend.domain.models.TipoManejoInventario.STOCK_EXACTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class DailyBuffetCloseRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private DailyBuffetCloseRepositoryImpl repository;

    @Autowired
    private SpringDataDailyBuffetCloseRepository springDataRepository;

    @Autowired
    private EntityManager entityManager;

    private ColegioEntity cachedSchool;
    private UsuarioEntity cachedUser;
    private BuffetEntity cachedBuffet;

    @Test
    void saveWithInventorySnapshot_ShouldPersistSnapshotWithClose() {
        DailyBuffetClose close = givenCloseDomainWithSnapshot();

        whenSavingCloseWithSnapshot(close);

        thenSnapshotIsPersisted(close.buffetId(), close.date());
    }

    @Test
    void findByBuffetAndDate_ShouldMapEntityToDomain() {
        CierreDiarioBuffetEntity entity = givenCloseEntityForYesterday();

        Optional<DailyBuffetClose> result = whenFindingByBuffetAndDate(entity.getBuffetEntity().getId(), entity.getFecha());

        thenCloseIsMapped(result, entity);
    }

    @Test
    void findByBuffetAndDateRange_ShouldMapEntitiesToDomain() {
        BuffetEntity buffet = givenBuffetEntityWithClose();

        thenFindingByDateRangeThrowsException(buffet.getId());
    }

    @Test
    void save_ShouldPersistCloseWithoutInventorySnapshot() {
        DailyBuffetClose close = givenCloseDomainWithoutSnapshot();

        DailyBuffetClose result = whenSavingClose(close);

        thenCloseIsPersisted(result, close.buffetId(), close.date());
    }

    @Test
    void findInventorySnapshotByBuffetAndDate_ShouldReturnSnapshotWhenPresent() {
        CierreDiarioBuffetEntity entity = givenCloseEntityWithSnapshot();

        Optional<List<DailyInventorySnapshot>> result = whenFindingSnapshotByBuffetAndDate(entity.getBuffetEntity().getId(), entity.getFecha());

        thenSnapshotIsReturned(result);
    }

    @Test
    void findInventorySnapshotByBuffetAndDate_ShouldReturnEmptyWhenSnapshotIsEmpty() {
        CierreDiarioBuffetEntity entity = givenCloseEntityWithoutSnapshot();

        Optional<List<DailyInventorySnapshot>> result = whenFindingSnapshotByBuffetAndDate(entity.getBuffetEntity().getId(), entity.getFecha());

        assertTrue(result.isEmpty());
    }

    private DailyBuffetClose givenCloseDomainWithSnapshot() {
        BuffetEntity buffet = generateBuffetEntity();
        return createCloseDomain(buffet.getId(), LocalDate.now());
    }

    private DailyBuffetClose givenCloseDomainWithoutSnapshot() {
        BuffetEntity buffet = generateBuffetEntity();
        return createCloseDomain(buffet.getId(), LocalDate.now().minusDays(2));
    }

    private CierreDiarioBuffetEntity givenCloseEntityForYesterday() {
        BuffetEntity buffet = generateBuffetEntity();
        return generateCloseEntity(buffet, LocalDate.now().minusDays(1));
    }

    private BuffetEntity givenBuffetEntityWithClose() {
        BuffetEntity buffet = generateBuffetEntity();
        generateCloseEntity(buffet, LocalDate.now().minusDays(4));
        return buffet;
    }

    private CierreDiarioBuffetEntity givenCloseEntityWithSnapshot() {
        BuffetEntity buffet = generateBuffetEntity();
        CierreDiarioBuffetEntity entity = generateCloseEntity(buffet, LocalDate.now().minusDays(3));
        entity.setInventarioSnapshotFromDomain(List.of(createSnapshotDomain()));
        springDataRepository.saveAndFlush(entity);
        entityManager.clear();
        return entity;
    }

    private CierreDiarioBuffetEntity givenCloseEntityWithoutSnapshot() {
        BuffetEntity buffet = generateBuffetEntity();
        CierreDiarioBuffetEntity entity = generateCloseEntity(buffet, LocalDate.now().minusDays(4));
        entityManager.clear();
        return entity;
    }

    private void whenSavingCloseWithSnapshot(DailyBuffetClose close) {
        DailyInventorySnapshot snapshot = createSnapshotDomain();
        repository.saveWithInventorySnapshot(close, List.of(snapshot));
        entityManager.flush();
        entityManager.clear();
    }

    private DailyBuffetClose whenSavingClose(DailyBuffetClose close) {
        DailyBuffetClose result = repository.save(close);
        entityManager.flush();
        entityManager.clear();
        return result;
    }

    private Optional<DailyBuffetClose> whenFindingByBuffetAndDate(UUID buffetId, LocalDate date) {
        return repository.findByBuffetAndDate(buffetId, date);
    }

    private void thenFindingByDateRangeThrowsException(UUID buffetId) {
        assertThrows(InvalidDataAccessResourceUsageException.class,
                () -> repository.findByBuffetAndDateRange(buffetId, LocalDate.now().minusDays(5), LocalDate.now()));
    }

    private Optional<List<DailyInventorySnapshot>> whenFindingSnapshotByBuffetAndDate(UUID buffetId, LocalDate date) {
        return repository.findInventorySnapshotByBuffetAndDate(buffetId, date);
    }

    private void thenSnapshotIsPersisted(UUID buffetId, LocalDate closeDate) {
        Optional<CierreDiarioBuffetEntity> savedEntity = springDataRepository.findByBuffetEntityIdAndFecha(buffetId, closeDate);
        assertTrue(savedEntity.isPresent());
        assertEquals(1, savedEntity.get().getInventarioSnapshot().size());
        assertEquals("Alfajor", savedEntity.get().getInventarioSnapshot().get(0).getProductoNombre());
    }

    private void thenCloseIsMapped(Optional<DailyBuffetClose> result, CierreDiarioBuffetEntity entity) {
        assertTrue(result.isPresent());
        assertEquals(entity.getBuffetEntity().getId(), result.get().buffetId());
        assertEquals(entity.getFecha(), result.get().date());
    }

    private void thenCloseIsPersisted(DailyBuffetClose result, UUID buffetId, LocalDate closeDate) {
        assertEquals(buffetId, result.buffetId());
        assertTrue(springDataRepository.findByBuffetEntityIdAndFecha(buffetId, closeDate).isPresent());
    }

    private void thenSnapshotIsReturned(Optional<List<DailyInventorySnapshot>> result) {
        assertTrue(result.isPresent());
        assertEquals(1, result.get().size());
    }

    private ColegioEntity generateSchoolEntity() {
        if (this.cachedSchool == null) {
            ColegioEntity school = new ColegioEntity();
            school.setNombre("Test School");
            school.setCue("CUE-DAILY-CLOSE");
            school.setActivo(true);
            entityManager.persist(school);
            this.cachedSchool = school;
        }
        return this.cachedSchool;
    }

    private UsuarioEntity generateUserEntity() {
        if (this.cachedUser == null) {
            UsuarioEntity user = new UsuarioEntity();
            user.setIdCognito("sub-daily-close");
            user.setNombre("Vendor Name");
            user.setApellido("Vendor Surname");
            user.setEmail("vendor.close@test.com");
            user.setRol(VENDEDOR);
            user.setActivo(true);
            entityManager.persist(user);
            this.cachedUser = user;
        }
        return this.cachedUser;
    }

    private BuffetEntity generateBuffetEntity() {
        if (this.cachedBuffet == null) {
            BuffetEntity buffet = new BuffetEntity();
            buffet.setColegioEntity(generateSchoolEntity());
            buffet.setNombre("Buffet Central");
            entityManager.persist(buffet);
            this.cachedBuffet = buffet;
        }
        return this.cachedBuffet;
    }

    private CierreDiarioBuffetEntity generateCloseEntity(BuffetEntity buffet, LocalDate date) {
        CierreDiarioBuffetEntity entity = new CierreDiarioBuffetEntity();
        entity.setBuffetEntity(buffet);
        entity.setFecha(date);
        entity.setPedidosVencidos(2);
        entity.setReservasLiberadas(3);
        entity.setCreditosDevueltos(BigDecimal.TEN);
        return springDataRepository.save(entity);
    }

    private DailyBuffetClose createCloseDomain(UUID buffetId, LocalDate date) {
        return new DailyBuffetClose(
                null,
                buffetId,
                date,
                1,
                2,
                BigDecimal.ZERO
        );
    }

    private DailyInventorySnapshot createSnapshotDomain() {
        return new DailyInventorySnapshot(
                UUID.randomUUID(),
                "Alfajor",
                10,
                2,
                8,
                3,
                null,
                null,
                DISPONIBLE,
                STOCK_EXACTO
        );
    }
}
