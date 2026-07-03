package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.IntervalUnit;
import com.recred.backend.domain.models.KiosqueroPayoutConfig;
import com.recred.backend.domain.models.PayoutScheduleStatus;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoPlan;
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
        KiosqueroEntity kiosquero = generateKiosquero();
        KiosqueroPayoutConfig config = createDomainConfig(kiosquero.getId());

        KiosqueroPayoutConfig saved = repository.save(config);

        assertNotNull(saved);
        assertEquals(kiosquero.getId(), saved.getKiosqueroId());

        Optional<KiosqueroPayoutConfigEntity> fromDb = springDataRepository.findById(kiosquero.getId());
        assertTrue(fromDb.isPresent());
        assertEquals("20123456789", fromDb.get().getDestinationCuit());
        assertEquals("cvu_123", fromDb.get().getDestinationCvu());
    }

    @Test
    void findByKiosqueroId_WhenConfigExists_ShouldReturnConfig() {
        KiosqueroEntity kiosquero = generateKiosquero();
        generateConfigEntity(kiosquero);

        Optional<KiosqueroPayoutConfig> result = repository.findByKiosqueroId(kiosquero.getId());

        assertTrue(result.isPresent());
        assertEquals("20123456789", result.get().getDestinationCuit());
        assertEquals("cvu_123", result.get().getDestinationCvu());
    }

    @Test
    void findByKiosqueroId_WhenConfigDoesNotExist_ShouldReturnEmpty() {
        KiosqueroEntity kiosquero = generateKiosquero();

        Optional<KiosqueroPayoutConfig> result = repository.findByKiosqueroId(kiosquero.getId());

        assertFalse(result.isPresent());
    }

    @Test
    void findActiveConfigsDueOnOrBefore_ShouldReturnMatchingConfigs() {
        KiosqueroEntity kiosquero = generateKiosquero();
        KiosqueroPayoutConfigEntity configEntity = generateConfigEntity(kiosquero);
        configEntity.setEstado(PayoutScheduleStatus.ACTIVE);
        configEntity.setProximaEjecucion(LocalDate.of(2026, 7, 1));
        entityManager.merge(configEntity);
        entityManager.flush();

        List<KiosqueroPayoutConfig> results = repository.findActiveConfigsDueOnOrBefore(
                LocalDate.of(2026, 7, 2));

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals(kiosquero.getId(), results.get(0).getKiosqueroId());
    }

    @Test
    void findActiveConfigsDueOnOrBefore_WhenNotActive_ShouldNotReturn() {
        KiosqueroEntity kiosquero = generateKiosquero();
        KiosqueroPayoutConfigEntity configEntity = generateConfigEntity(kiosquero);
        configEntity.setEstado(PayoutScheduleStatus.PAUSED);
        configEntity.setProximaEjecucion(LocalDate.of(2026, 7, 1));
        entityManager.merge(configEntity);
        entityManager.flush();

        List<KiosqueroPayoutConfig> results = repository.findActiveConfigsDueOnOrBefore(
                LocalDate.of(2026, 7, 2));

        assertTrue(results.isEmpty());
    }

    @Test
    void findActiveConfigsDueOnOrBefore_WhenFutureDate_ShouldNotReturn() {
        KiosqueroEntity kiosquero = generateKiosquero();
        KiosqueroPayoutConfigEntity configEntity = generateConfigEntity(kiosquero);
        configEntity.setEstado(PayoutScheduleStatus.ACTIVE);
        configEntity.setProximaEjecucion(LocalDate.of(2026, 7, 5));
        entityManager.merge(configEntity);
        entityManager.flush();

        List<KiosqueroPayoutConfig> results = repository.findActiveConfigsDueOnOrBefore(
                LocalDate.of(2026, 7, 2));

        assertTrue(results.isEmpty());
    }

    private KiosqueroPayoutConfig createDomainConfig(UUID kiosqueroId) {
        KiosqueroPayoutConfig config = new KiosqueroPayoutConfig();
        config.setKiosqueroId(kiosqueroId);
        config.setDestinationCuit("20123456789");
        config.setDestinationCvu("cvu_123");
        config.setAccountHolderName("John Doe");
        config.setUnidadIntervalo(IntervalUnit.DAYS);
        config.setCantidadIntervalo(1);
        config.setEstado(PayoutScheduleStatus.ACTIVE);
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
        entity.setUnidadIntervalo(IntervalUnit.DAYS);
        entity.setCantidadIntervalo(1);
        entity.setEstado(PayoutScheduleStatus.ACTIVE);
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
        usuario.setRol(RolUsuario.VENDEDOR);
        usuario.setAutenticadoConCognito(true);
        usuario.setActivo(true);
        usuario.setPlan(TipoPlan.GRATUITO);
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
