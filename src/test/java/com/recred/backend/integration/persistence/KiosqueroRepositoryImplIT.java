package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.RolUsuario;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private UsuarioEntity cachedUser;

    @Test
    void findBuffetById_ShouldReturnBuffetId() {
        KiosqueroEntity kiosquero = generateKiosqueroEntity("20-12345678-9", "vendor1.test@recred.com");

        UUID result = repository.findBuffetById(kiosquero.getId());

        assertEquals(kiosquero.getBuffetEntity().getId(), result);
    }

    @Test
    void findBuffetByUsuarioId_ShouldReturnBuffetId() {
        KiosqueroEntity kiosquero = generateKiosqueroEntity("20-12345678-9", "vendor2.test@recred.com");

        UUID result = repository.findBuffetByUsuarioId(kiosquero.getUsuario().getId());

        assertEquals(kiosquero.getBuffetEntity().getId(), result);
    }

    @Test
    void findAllIds_ShouldReturnAllKiosqueroIds() {
        KiosqueroEntity first = generateKiosqueroEntity("20-12345678-9", "vendor3.test@recred.com");
        KiosqueroEntity second = generateKiosqueroEntity("20-87654321-9", "vendor4.test@recred.com");

        List<UUID> result = repository.findAllIds();

        assertTrue(result.contains(first.getId()));
        assertTrue(result.contains(second.getId()));
    }

    @Test
    void findAll_ShouldMapEntitiesToDomain() {
        KiosqueroEntity first = generateKiosqueroEntity("20-12345678-9", "vendor5.test@recred.com");
        KiosqueroEntity second = generateKiosqueroEntity("20-87654321-9", "vendor6.test@recred.com");

        List<Kiosquero> result = repository.findAll();

        assertTrue(result.size() >= 2);
        assertTrue(result.stream().anyMatch(k -> k.getId().equals(first.getId()) && k.getCuit().equals("20-12345678-9")));
        assertTrue(result.stream().anyMatch(k -> k.getId().equals(second.getId()) && k.getCuit().equals("20-87654321-9")));
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
        entity.setBuffetEntity(generateBuffetEntity());
        return springDataKiosqueroRepository.save(entity);
    }
}
