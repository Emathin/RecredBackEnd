package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.KiosqueroPayoutConfig;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.KiosqueroEntity;
import com.recred.backend.infrastructure.entities.KiosqueroPayoutConfigEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.KiosqueroPayoutConfigJpaRepository;
import com.recred.backend.infrastructure.persistence.KiosqueroPayoutConfigRepositoryImpl;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.recred.backend.domain.models.IntervalUnit.DAYS;
import static com.recred.backend.domain.models.PayoutScheduleStatus.ACTIVE;
import static com.recred.backend.domain.models.PayoutScheduleStatus.PAUSED;
import static com.recred.backend.domain.models.RolUsuario.VENDEDOR;
import static com.recred.backend.domain.models.TipoPlan.GRATUITO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class KiosqueroPayoutConfigRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private KiosqueroPayoutConfigRepositoryImpl repository;

    @Autowired
    private KiosqueroPayoutConfigJpaRepository springDataRepository;

    @Autowired
    private EntityManager entityManager;

    private KiosqueroEntity cachedKiosquero;

    @Test
    void save_ShouldPersistNewConfig() {
        KiosqueroPayoutConfig config = givenNewPayoutConfigDomain();

        KiosqueroPayoutConfig saved = whenSavingConfig(config);

        thenConfigIsPersisted(saved, config.getKiosqueroId());
    }

    @Test
    void findByKiosqueroId_WhenConfigExists_ShouldReturnConfig() {
        KiosqueroEntity kiosquero = givenPersistedConfigForKiosquero();

        Optional<KiosqueroPayoutConfig> result = whenFindingConfigById(kiosquero.getId());

        thenConfigIsFound(result);
    }

    @Test
    void findByKiosqueroId_WhenConfigDoesNotExist_ShouldReturnEmpty() {
        KiosqueroEntity kiosquero = givenKiosqueroWithoutConfig();

        Optional<KiosqueroPayoutConfig> result = whenFindingConfigById(kiosquero.getId());

        thenConfigIsNotFound(result);
    }

    @Test
    void findActiveConfigsDueOnOrBefore_ShouldReturnMatchingConfigs() {
        KiosqueroEntity kiosquero = givenActiveConfigDueYesterday();

        List<KiosqueroPayoutConfig> results = whenFindingConfigsDueToday();

        thenActiveConfigIsReturned(results, kiosquero.getId());
    }

    @Test
    void findActiveConfigsDueOnOrBefore_WhenNotActive_ShouldNotReturn() {
        givenPausedConfigDueYesterday();

        List<KiosqueroPayoutConfig> results = whenFindingConfigsDueToday();

        thenNoConfigsAreReturned(results);
    }

    @Test
    void findActiveConfigsDueOnOrBefore_WhenFutureDate_ShouldNotReturn() {
        givenActiveConfigDueNextWeek();

        List<KiosqueroPayoutConfig> results = whenFindingConfigsDueToday();

        thenNoConfigsAreReturned(results);
    }

    private KiosqueroPayoutConfig givenNewPayoutConfigDomain() {
        KiosqueroEntity kiosquero = generateKiosquero();
        return createDomainConfig(kiosquero.getId());
    }

    private KiosqueroEntity givenPersistedConfigForKiosquero() {
        KiosqueroEntity kiosquero = generateKiosquero();
        generateConfigEntity(kiosquero);
        return kiosquero;
    }

    private KiosqueroEntity givenKiosqueroWithoutConfig() {
        return generateKiosquero();
    }

    private KiosqueroEntity givenActiveConfigDueYesterday() {
        KiosqueroEntity kiosquero = generateKiosquero();
        KiosqueroPayoutConfigEntity configEntity = generateConfigEntity(kiosquero);
        configEntity.setEstado(ACTIVE);
        configEntity.setProximaEjecucion(LocalDate.now().minusDays(1));
        entityManager.merge(configEntity);
        entityManager.flush();
        return kiosquero;
    }

    private void givenPausedConfigDueYesterday() {
        KiosqueroEntity kiosquero = generateKiosquero();
        KiosqueroPayoutConfigEntity configEntity = generateConfigEntity(kiosquero);
        configEntity.setEstado(PAUSED);
        configEntity.setProximaEjecucion(LocalDate.now().minusDays(1));
        entityManager.merge(configEntity);
        entityManager.flush();
    }

    private void givenActiveConfigDueNextWeek() {
        KiosqueroEntity kiosquero = generateKiosquero();
        KiosqueroPayoutConfigEntity configEntity = generateConfigEntity(kiosquero);
        configEntity.setEstado(ACTIVE);
        configEntity.setProximaEjecucion(LocalDate.now().plusDays(7));
        entityManager.merge(configEntity);
        entityManager.flush();
    }

    private KiosqueroPayoutConfig whenSavingConfig(KiosqueroPayoutConfig config) {
        return repository.save(config);
    }

    private Optional<KiosqueroPayoutConfig> whenFindingConfigById(UUID kiosqueroId) {
        return repository.findByKiosqueroId(kiosqueroId);
    }

    private List<KiosqueroPayoutConfig> whenFindingConfigsDueToday() {
        return repository.findActiveConfigsDueOnOrBefore(LocalDate.now());
    }

    private void thenConfigIsPersisted(KiosqueroPayoutConfig saved, UUID expectedKiosqueroId) {
        assertNotNull(saved);
        assertEquals(expectedKiosqueroId, saved.getKiosqueroId());

        Optional<KiosqueroPayoutConfigEntity> fromDb = springDataRepository.findById(expectedKiosqueroId);
        assertTrue(fromDb.isPresent());
        assertEquals("20123456789", fromDb.get().getDestinationCuit());
        assertEquals("cvu_123", fromDb.get().getDestinationCvu());
    }

    private void thenConfigIsFound(Optional<KiosqueroPayoutConfig> result) {
        assertTrue(result.isPresent());
        assertEquals("20123456789", result.get().getDestinationCuit());
        assertEquals("cvu_123", result.get().getDestinationCvu());
    }

    private void thenConfigIsNotFound(Optional<KiosqueroPayoutConfig> result) {
        assertFalse(result.isPresent());
    }

    private void thenActiveConfigIsReturned(List<KiosqueroPayoutConfig> results, UUID expectedKiosqueroId) {
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals(expectedKiosqueroId, results.get(0).getKiosqueroId());
    }

    private void thenNoConfigsAreReturned(List<KiosqueroPayoutConfig> results) {
        assertTrue(results.isEmpty());
    }

    private KiosqueroPayoutConfig createDomainConfig(UUID kiosqueroId) {
        KiosqueroPayoutConfig config = new KiosqueroPayoutConfig();
        config.setKiosqueroId(kiosqueroId);
        config.setDestinationCuit("20123456789");
        config.setDestinationCvu("cvu_123");
        config.setAccountHolderName("John Doe");
        config.setUnidadIntervalo(DAYS);
        config.setCantidadIntervalo(1);
        config.setEstado(ACTIVE);
        config.setProximaEjecucion(LocalDate.now());
        return config;
    }

    private KiosqueroPayoutConfigEntity generateConfigEntity(KiosqueroEntity kiosquero) {
        KiosqueroPayoutConfigEntity entity = new KiosqueroPayoutConfigEntity();
        entity.setKiosqueroId(kiosquero.getId());
        entity.setKiosquero(kiosquero);
        entity.setDestinationCuit("20123456789");
        entity.setDestinationCvu("cvu_123");
        entity.setAccountHolderName("John Doe");
        entity.setUnidadIntervalo(DAYS);
        entity.setCantidadIntervalo(1);
        entity.setEstado(ACTIVE);
        entity.setProximaEjecucion(LocalDate.now());
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }

    private KiosqueroEntity generateKiosquero() {
        if (cachedKiosquero != null) {
            return cachedKiosquero;
        }

        ColegioEntity colegio = new ColegioEntity();
        colegio.setNombre("Test School");
        colegio.setCue(UUID.randomUUID().toString());
        colegio.setActivo(true);
        entityManager.persist(colegio);

        BuffetEntity buffet = new BuffetEntity();
        buffet.setColegioEntity(colegio);
        buffet.setNombre("Test Buffet");
        buffet.setActivo(true);
        entityManager.persist(buffet);

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setIdCognito(UUID.randomUUID().toString());
        usuario.setNombre("Test");
        usuario.setApellido("User");
        usuario.setEmail(UUID.randomUUID().toString() + "@test.com");
        usuario.setRol(VENDEDOR);
        usuario.setAutenticadoConCognito(true);
        usuario.setActivo(true);
        usuario.setPlan(GRATUITO);
        entityManager.persist(usuario);

        KiosqueroEntity kiosquero = new KiosqueroEntity();
        kiosquero.setUsuario(usuario);
        kiosquero.setCuit("20123456789");
        kiosquero.setBuffetEntity(buffet);
        entityManager.persist(kiosquero);

        entityManager.flush();
        cachedKiosquero = kiosquero;
        return kiosquero;
    }
}
