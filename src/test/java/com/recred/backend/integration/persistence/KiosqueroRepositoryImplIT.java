package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.KiosqueroEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.KiosqueroRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataKiosqueroRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class KiosqueroRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private KiosqueroRepositoryImpl repository;

    @Autowired
    private SpringDataKiosqueroRepository springDataKiosqueroRepository;

    @Autowired
    private EntityManager entityManager;

    private ColegioEntity cachedSchool;
    private BuffetEntity cachedBuffet;

    @Test
    void findBuffetById_ShouldReturnBuffetId() {
        KiosqueroEntity kiosquero = givenPersistedKiosquero();

        UUID result = whenFindingBuffetById(kiosquero.getId());

        thenBuffetIdMatches(result, kiosquero.getBuffetEntity().getId());
    }

    @Test
    void findBuffetByUsuarioId_ShouldReturnBuffetId() {
        KiosqueroEntity kiosquero = givenPersistedKiosquero();

        UUID result = whenFindingBuffetByUserId(kiosquero.getUsuario().getId());

        thenBuffetIdMatches(result, kiosquero.getBuffetEntity().getId());
    }

    @Test
    void findSchoolIdByUsuarioId_ShouldReturnSchoolId() {
        KiosqueroEntity kiosquero = givenPersistedKiosquero();

        Optional<UUID> result = whenFindingSchoolIdByUserId(kiosquero.getUsuario().getId());

        thenSchoolIdMatches(result, kiosquero.getBuffetEntity().getColegioEntity().getId());
    }

    @Test
    void findUsuarioIdById_ShouldReturnUsuarioId() {
        KiosqueroEntity kiosquero = givenPersistedKiosquero();

        UUID result = whenFindingUserIdById(kiosquero.getId());

        thenUserIdMatches(result, kiosquero.getUsuario().getId());
    }

    @Test
    void findAllIds_ShouldReturnAllKiosqueroIds() {
        KiosqueroEntity first = givenPersistedKiosquero();
        KiosqueroEntity second = givenPersistedKiosquero();

        List<UUID> result = whenFindingAllIds();

        thenAllIdsContain(result, first.getId(), second.getId());
    }

    @Test
    void findAll_ShouldMapEntitiesToDomain() {
        KiosqueroEntity first = givenPersistedKiosquero();
        KiosqueroEntity second = givenPersistedKiosquero();

        List<Kiosquero> result = whenFindingAllKiosqueros();

        thenAllKiosquerosContain(result, first, second);
    }

    @Test
    void save_ShouldSaveInRealDatabaseAndGenerateId() {
        Kiosquero domainKiosquero = givenNewKiosqueroDomain();

        Kiosquero savedKiosquero = whenSavingKiosquero(domainKiosquero);

        thenKiosqueroIsPersistedCorrectly(savedKiosquero, domainKiosquero);
    }

    private KiosqueroEntity givenPersistedKiosquero() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        return generateKiosqueroEntity("20-" + uniqueSuffix + "-9", "vendor" + uniqueSuffix + "@recred.com");
    }

    private Kiosquero givenNewKiosqueroDomain() {
        UsuarioEntity userEntity = generateUserEntity("new.vendor-" + UUID.randomUUID() + "@test.com");
        BuffetEntity buffetEntity = generateBuffetEntity();

        Kiosquero domainKiosquero = new Kiosquero();
        domainKiosquero.setCuit("20-99999999-9");
        
        Usuario domainUser = new Usuario();
        domainUser.setId(userEntity.getId());
        domainKiosquero.setUsuario(domainUser);
        
        Buffet domainBuffet = new Buffet();
        domainBuffet.setId(buffetEntity.getId());
        domainKiosquero.setBuffet(domainBuffet);
        
        return domainKiosquero;
    }

    private UUID whenFindingBuffetById(UUID kiosqueroId) {
        return repository.findBuffetById(kiosqueroId);
    }

    private UUID whenFindingBuffetByUserId(UUID userId) {
        return repository.findBuffetByUsuarioId(userId);
    }

    private Optional<UUID> whenFindingSchoolIdByUserId(UUID userId) {
        return repository.findSchoolIdByUsuarioId(userId);
    }

    private UUID whenFindingUserIdById(UUID kiosqueroId) {
        return repository.findUsuarioIdById(kiosqueroId);
    }

    private List<UUID> whenFindingAllIds() {
        return repository.findAllIds();
    }

    private List<Kiosquero> whenFindingAllKiosqueros() {
        return repository.findAll();
    }

    private Kiosquero whenSavingKiosquero(Kiosquero domainKiosquero) {
        return repository.save(domainKiosquero);
    }

    private void thenBuffetIdMatches(UUID result, UUID expectedId) {
        assertEquals(expectedId, result);
    }

    private void thenSchoolIdMatches(Optional<UUID> result, UUID expectedId) {
        assertTrue(result.isPresent());
        assertEquals(expectedId, result.get());
    }

    private void thenUserIdMatches(UUID result, UUID expectedId) {
        assertEquals(expectedId, result);
    }

    private void thenAllIdsContain(List<UUID> result, UUID firstId, UUID secondId) {
        assertTrue(result.contains(firstId));
        assertTrue(result.contains(secondId));
    }

    private void thenAllKiosquerosContain(List<Kiosquero> result, KiosqueroEntity first, KiosqueroEntity second) {
        assertTrue(result.size() >= 2);
        assertTrue(result.stream().anyMatch(k -> k.getId().equals(first.getId()) && k.getCuit().equals(first.getCuit())));
        assertTrue(result.stream().anyMatch(k -> k.getId().equals(second.getId()) && k.getCuit().equals(second.getCuit())));
    }

    private void thenKiosqueroIsPersistedCorrectly(Kiosquero savedKiosquero, Kiosquero domainKiosquero) {
        assertNotNull(savedKiosquero.getId());

        Optional<KiosqueroEntity> dbKiosquero = springDataKiosqueroRepository.findById(savedKiosquero.getId());
        assertTrue(dbKiosquero.isPresent());
        assertEquals(domainKiosquero.getCuit(), dbKiosquero.get().getCuit());
        assertEquals(domainKiosquero.getUsuario().getId(), dbKiosquero.get().getUsuario().getId());
        assertEquals(domainKiosquero.getBuffet().getId(), dbKiosquero.get().getBuffetEntity().getId());
    }

    private ColegioEntity generateSchoolEntity() {
        if (this.cachedSchool == null) {
            ColegioEntity school = new ColegioEntity();
            school.setNombre("Test School");
            school.setCue("CUE-KIOSQUERO-123");
            school.setActivo(true);
            entityManager.persist(school);
            this.cachedSchool = school;
        }
        return this.cachedSchool;
    }

    private BuffetEntity generateBuffetEntity() {
        if (this.cachedBuffet == null) {
            BuffetEntity buffet = new BuffetEntity();
            buffet.setColegioEntity(generateSchoolEntity());
            buffet.setNombre("Buffet Kiosquero");
            buffet.setActivo(true);
            entityManager.persist(buffet);
            this.cachedBuffet = buffet;
        }
        return this.cachedBuffet;
    }

    private UsuarioEntity generateUserEntity(String email) {
        UsuarioEntity user = new UsuarioEntity();
        user.setIdCognito("sub-kiosquero-" + UUID.randomUUID().toString().substring(0, 8));
        user.setNombre("Vendor");
        user.setApellido("User");
        user.setEmail(email);
        user.setRol(RolUsuario.VENDEDOR);
        user.setActivo(true);
        entityManager.persist(user);
        return user;
    }

    private KiosqueroEntity generateKiosqueroEntity(String cuit, String email) {
        KiosqueroEntity entity = new KiosqueroEntity();
        entity.setUsuario(generateUserEntity(email));
        entity.setCuit(cuit);
        BuffetEntity buffet = generateBuffetEntity();
        entity.setBuffetEntity(buffet);
        KiosqueroEntity savedKiosquero = springDataKiosqueroRepository.save(entity);
        
        buffet.setVendedor(savedKiosquero);
        entityManager.merge(buffet);
        
        return savedKiosquero;
    }
}
