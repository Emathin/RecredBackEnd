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

    private DailyBuffetClose cachedCloseDomain;
    private CierreDiarioBuffetEntity cachedEntity;

    @Test
    void saveWithInventorySnapshot_ShouldPersistSnapshotWithClose() {
        givenCloseDomainWithSnapshot();

        whenSavingCloseWithSnapshot();

        thenSnapshotIsPersisted();
    }

    @Test
    void findByBuffetAndDate_ShouldMapEntityToDomain() {
        givenCloseEntityForYesterday();

        Optional<DailyBuffetClose> result = whenFindingByBuffetAndDate();

        thenCloseIsMapped(result);
    }

    @Test
    void findByBuffetAndDateRange_ShouldMapEntitiesToDomain() {
        givenBuffetWithCloseInDateRange();

        List<DailyBuffetClose> result = whenFindingByBuffetAndDateRange();

        thenCloseIsMappedCorrectlyInRange(result);
    }

    @Test
    void findByBuffetAndDateRange_WhenDatesAreNull_ShouldReturnAllOrderedByDateDesc() {
        givenBuffetWithMultipleCloses();

        List<DailyBuffetClose> result = whenFindingByBuffetAndNullDateRange();

        thenClosesAreOrderedByDateDesc(result);
    }

    @Test
    void save_ShouldPersistCloseWithoutInventorySnapshot() {
        givenCloseDomainWithoutSnapshot();

        DailyBuffetClose result = whenSavingClose();

        thenCloseIsPersisted(result);
    }

    @Test
    void findInventorySnapshotByBuffetAndDate_ShouldReturnSnapshotWhenPresent() {
        givenCloseEntityWithSnapshot();

        Optional<List<DailyInventorySnapshot>> result = whenFindingSnapshotByBuffetAndDate();

        thenSnapshotIsReturned(result);
    }

    @Test
    void findInventorySnapshotByBuffetAndDate_ShouldReturnEmptyWhenSnapshotIsEmpty() {
        givenCloseEntityWithoutSnapshot();

        Optional<List<DailyInventorySnapshot>> result = whenFindingSnapshotByBuffetAndDate();

        thenSnapshotIsEmpty(result);
    }

    private void givenCloseDomainWithSnapshot() {
        BuffetEntity buffet = generateBuffetEntity();
        this.cachedCloseDomain = createCloseDomain(buffet.getId(), LocalDate.now());
    }

    private void givenCloseDomainWithoutSnapshot() {
        BuffetEntity buffet = generateBuffetEntity();
        this.cachedCloseDomain = createCloseDomain(buffet.getId(), LocalDate.now().minusDays(2));
    }

    private void givenCloseEntityForYesterday() {
        BuffetEntity buffet = generateBuffetEntity();
        this.cachedEntity = generateCloseEntity(buffet, LocalDate.now().minusDays(1));
    }

    private LocalDate cachedStartDate;
    private LocalDate cachedEndDate;
    private LocalDate cachedOlderCloseDate;
    private LocalDate cachedNewerCloseDate;
    private LocalDate cachedCloseDate;

    private void givenBuffetWithCloseInDateRange() {
        BuffetEntity buffet = generateBuffetEntity();
        this.cachedStartDate = LocalDate.now().minusDays(5);
        this.cachedEndDate = LocalDate.now();
        this.cachedCloseDate = this.cachedStartDate.plusDays(1);
        generateCloseEntity(buffet, this.cachedCloseDate);
        this.cachedBuffet = buffet;
    }

    private void givenBuffetWithMultipleCloses() {
        BuffetEntity buffet = generateBuffetEntity();
        this.cachedOlderCloseDate = LocalDate.now().minusDays(7);
        this.cachedNewerCloseDate = LocalDate.now().minusDays(6);
        generateCloseEntity(buffet, this.cachedOlderCloseDate);
        generateCloseEntity(buffet, this.cachedNewerCloseDate);
        this.cachedBuffet = buffet;
    }

    private void givenCloseEntityWithSnapshot() {
        BuffetEntity buffet = generateBuffetEntity();
        CierreDiarioBuffetEntity entity = generateCloseEntity(buffet, LocalDate.now().minusDays(3));
        entity.setInventarioSnapshotFromDomain(List.of(createSnapshotDomain()));
        springDataRepository.saveAndFlush(entity);
        entityManager.clear();
        this.cachedEntity = entity;
    }

    private void givenCloseEntityWithoutSnapshot() {
        BuffetEntity buffet = generateBuffetEntity();
        CierreDiarioBuffetEntity entity = generateCloseEntity(buffet, LocalDate.now().minusDays(4));
        entityManager.clear();
        this.cachedEntity = entity;
    }

    private void whenSavingCloseWithSnapshot() {
        DailyInventorySnapshot snapshot = createSnapshotDomain();
        repository.saveWithInventorySnapshot(this.cachedCloseDomain, List.of(snapshot));
        entityManager.flush();
        entityManager.clear();
    }

    private DailyBuffetClose whenSavingClose() {
        DailyBuffetClose result = repository.save(this.cachedCloseDomain);
        entityManager.flush();
        entityManager.clear();
        return result;
    }

    private Optional<DailyBuffetClose> whenFindingByBuffetAndDate() {
        return repository.findByBuffetAndDate(this.cachedEntity.getBuffetEntity().getId(), this.cachedEntity.getFecha());
    }

    private List<DailyBuffetClose> whenFindingByBuffetAndDateRange() {
        return repository.findByBuffetAndDateRange(this.cachedBuffet.getId(), this.cachedStartDate, this.cachedEndDate);
    }

    private List<DailyBuffetClose> whenFindingByBuffetAndNullDateRange() {
        return repository.findByBuffetAndDateRange(this.cachedBuffet.getId(), null, null);
    }

    private void thenCloseIsMappedCorrectlyInRange(List<DailyBuffetClose> result) {
        assertEquals(1, result.size());
        assertEquals(this.cachedCloseDate, result.get(0).date());
    }

    private void thenClosesAreOrderedByDateDesc(List<DailyBuffetClose> result) {
        assertEquals(2, result.size());
        assertEquals(this.cachedNewerCloseDate, result.get(0).date());
        assertEquals(this.cachedOlderCloseDate, result.get(1).date());
    }

    private Optional<List<DailyInventorySnapshot>> whenFindingSnapshotByBuffetAndDate() {
        return repository.findInventorySnapshotByBuffetAndDate(this.cachedEntity.getBuffetEntity().getId(), this.cachedEntity.getFecha());
    }

    private void thenSnapshotIsPersisted() {
        Optional<CierreDiarioBuffetEntity> savedEntity = springDataRepository.findByBuffetEntityIdAndFecha(this.cachedCloseDomain.buffetId(), this.cachedCloseDomain.date());
        assertTrue(savedEntity.isPresent());
        assertEquals(1, savedEntity.get().getInventarioSnapshot().size());
        assertEquals("Alfajor", savedEntity.get().getInventarioSnapshot().get(0).getProductoNombre());
    }

    private void thenCloseIsMapped(Optional<DailyBuffetClose> result) {
        assertTrue(result.isPresent());
        assertEquals(this.cachedEntity.getBuffetEntity().getId(), result.get().buffetId());
        assertEquals(this.cachedEntity.getFecha(), result.get().date());
    }

    private void thenCloseIsPersisted(DailyBuffetClose result) {
        assertEquals(this.cachedCloseDomain.buffetId(), result.buffetId());
        assertTrue(springDataRepository.findByBuffetEntityIdAndFecha(this.cachedCloseDomain.buffetId(), this.cachedCloseDomain.date()).isPresent());
    }

    private void thenSnapshotIsReturned(Optional<List<DailyInventorySnapshot>> result) {
        assertTrue(result.isPresent());
        assertEquals(1, result.get().size());
    }
    
    private void thenSnapshotIsEmpty(Optional<List<DailyInventorySnapshot>> result) {
        assertTrue(result.isEmpty());
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
