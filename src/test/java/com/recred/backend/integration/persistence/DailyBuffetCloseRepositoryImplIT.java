package com.recred.backend.integration.persistence;

import com.recred.backend.integration.AbstractIntegrationTest;

import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.models.DailyInventorySnapshot;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.CierreDiarioBuffetEntity;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.infrastructure.persistence.DailyBuffetCloseRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataDailyBuffetCloseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.test.context.TestPropertySource;

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
        BuffetEntity buffet = generateBuffetEntity();
        LocalDate closeDate = LocalDate.now();
        DailyBuffetClose close = createCloseDomain(buffet.getId(), closeDate);
        DailyInventorySnapshot snapshot = createSnapshotDomain();

        repository.saveWithInventorySnapshot(close, List.of(snapshot));
        entityManager.flush();
        entityManager.clear();

        Optional<CierreDiarioBuffetEntity> savedEntity = springDataRepository
                .findByBuffetEntityIdAndFecha(buffet.getId(), closeDate);
        
        assertTrue(savedEntity.isPresent());
        assertEquals(1, savedEntity.get().getInventarioSnapshot().size());
        assertEquals(snapshot.productName(), savedEntity.get().getInventarioSnapshot().get(0).getProductoNombre());
    }

    @Test
    void findByBuffetAndDate_ShouldMapEntityToDomain() {
        BuffetEntity buffet = generateBuffetEntity();
        LocalDate closeDate = LocalDate.now().minusDays(1);
        CierreDiarioBuffetEntity entity = generateCloseEntity(buffet, closeDate);

        Optional<DailyBuffetClose> result = repository.findByBuffetAndDate(buffet.getId(), closeDate);

        assertTrue(result.isPresent());
        assertEquals(buffet.getId(), result.get().buffetId());
        assertEquals(closeDate, result.get().date());
    }

    @Test
    void findByBuffetAndDateRange_ShouldMapEntitiesToDomain() {
        BuffetEntity buffet = generateBuffetEntity();
        LocalDate startDate = LocalDate.now().minusDays(5);
        LocalDate endDate = LocalDate.now();
        LocalDate closeDate = startDate.plusDays(1);
        generateCloseEntity(buffet, closeDate);

        List<DailyBuffetClose> result = repository.findByBuffetAndDateRange(
                buffet.getId(),
                startDate,
                endDate);

        assertEquals(1, result.size());
        assertEquals(closeDate, result.get(0).date());
    }

    @Test
    void findByBuffetAndDateRange_WhenDatesAreNull_ShouldReturnAllOrderedByDateDesc() {
        BuffetEntity buffet = generateBuffetEntity();
        LocalDate olderCloseDate = LocalDate.now().minusDays(7);
        LocalDate newerCloseDate = LocalDate.now().minusDays(6);
        generateCloseEntity(buffet, olderCloseDate);
        generateCloseEntity(buffet, newerCloseDate);

        List<DailyBuffetClose> result = repository.findByBuffetAndDateRange(
                buffet.getId(),
                null,
                null);

        assertEquals(2, result.size());
        assertEquals(newerCloseDate, result.get(0).date());
        assertEquals(olderCloseDate, result.get(1).date());
    }

    @Test
    void save_ShouldPersistCloseWithoutInventorySnapshot() {
        BuffetEntity buffet = generateBuffetEntity();
        LocalDate closeDate = LocalDate.now().minusDays(2);
        DailyBuffetClose close = createCloseDomain(buffet.getId(), closeDate);

        DailyBuffetClose result = repository.save(close);
        entityManager.flush();
        entityManager.clear();

        assertEquals(close.buffetId(), result.buffetId());
        assertTrue(springDataRepository.findByBuffetEntityIdAndFecha(buffet.getId(), closeDate).isPresent());
    }

    @Test
    void findInventorySnapshotByBuffetAndDate_ShouldReturnSnapshotWhenPresent() {
        BuffetEntity buffet = generateBuffetEntity();
        LocalDate closeDate = LocalDate.now().minusDays(3);
        CierreDiarioBuffetEntity entity = generateCloseEntity(buffet, closeDate);
        entity.setInventarioSnapshotFromDomain(List.of(createSnapshotDomain()));
        springDataRepository.saveAndFlush(entity);
        entityManager.clear();

        Optional<List<DailyInventorySnapshot>> result = repository.findInventorySnapshotByBuffetAndDate(buffet.getId(), closeDate);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().size());
    }

    @Test
    void findInventorySnapshotByBuffetAndDate_ShouldReturnEmptyWhenSnapshotIsEmpty() {
        BuffetEntity buffet = generateBuffetEntity();
        LocalDate closeDate = LocalDate.now().minusDays(4);
        generateCloseEntity(buffet, closeDate);
        entityManager.clear();

        Optional<List<DailyInventorySnapshot>> result = repository.findInventorySnapshotByBuffetAndDate(buffet.getId(), closeDate);

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
            user.setRol(RolUsuario.VENDEDOR);
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
                EstadoInventario.DISPONIBLE,
                TipoManejoInventario.STOCK_EXACTO
        );
    }
}
