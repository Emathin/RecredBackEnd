package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.infrastructure.entities.DispositivoUsuarioEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.UserDeviceTokenJpaRepository;
import com.recred.backend.infrastructure.persistence.UserDeviceTokenPersistenceRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class UserDeviceTokenPersistenceRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private UserDeviceTokenPersistenceRepository repository;

    @Autowired
    private UserDeviceTokenJpaRepository springDataRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void returnOnlyNonBlankActiveTokens() {
        UsuarioEntity user = generateUser(RolUsuario.PADRE);

        createToken(user, "token-1", true);
        createToken(user, "", true);
        createToken(user, " ", true);
        createToken(user, "token-2", true);
        createToken(user, "token-3", false);

        entityManager.flush();
        entityManager.clear();

        List<String> tokens = repository.findActiveTokensByUserId(user.getId());

        assertEquals(List.of("token-1", "token-2"), tokens);
    }

    @Test
    void deactivateTokens_WhenListHasValues_ShouldDelegate() {
        UsuarioEntity user = generateUser(RolUsuario.PADRE);
        createToken(user, "token-1", true);
        createToken(user, "token-2", true);
        createToken(user, "token-3", true);
        entityManager.flush();
        entityManager.clear();

        List<String> tokensToDeactivate = List.of("token-1", "token-2");
        repository.deactivateTokens(tokensToDeactivate);
        entityManager.flush();
        entityManager.clear();

        DispositivoUsuarioEntity token1 = springDataRepository.findByFcmToken("token-1").orElseThrow();
        assertFalse(token1.getActivo());

        DispositivoUsuarioEntity token2 = springDataRepository.findByFcmToken("token-2").orElseThrow();
        assertFalse(token2.getActivo());

        DispositivoUsuarioEntity token3 = springDataRepository.findByFcmToken("token-3").orElseThrow();
        assertTrue(token3.getActivo());
    }

    @Test
    void deactivateTokens_WhenListIsNullOrEmpty_ShouldDoNothing() {
        UsuarioEntity user = generateUser(RolUsuario.PADRE);
        createToken(user, "token-1", true);
        entityManager.flush();
        entityManager.clear();

        repository.deactivateTokens(null);
        repository.deactivateTokens(List.of());
        entityManager.flush();
        entityManager.clear();

        DispositivoUsuarioEntity token1 = springDataRepository.findByFcmToken("token-1").orElseThrow();
        assertTrue(token1.getActivo());
    }

    private UsuarioEntity generateUser(RolUsuario role) {
        UsuarioEntity user = new UsuarioEntity();
        user.setIdCognito("sub-" + UUID.randomUUID().toString().substring(0, 8));
        user.setNombre("Test");
        user.setApellido("User");
        user.setEmail("user" + UUID.randomUUID().toString().substring(0, 5) + "@test.com");
        user.setRol(role);
        user.setActivo(true);
        entityManager.persist(user);
        return user;
    }

    private void createToken(UsuarioEntity user, String token, boolean active) {
        DispositivoUsuarioEntity entity = new DispositivoUsuarioEntity();
        entity.setUsuarioEntity(user);
        entity.setFcmToken(token);
        entity.setActivo(active);
        springDataRepository.save(entity);
    }
}
