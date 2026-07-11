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

    private UsuarioEntity cachedUser;

    @Test
    void returnOnlyNonBlankActiveTokens() {
        givenUserWithVariousTokens();

        List<String> tokens = whenFindingActiveTokens();

        thenOnlyValidActiveTokensAreReturned(tokens);
    }

    @Test
    void deactivateTokens_WhenListHasValues_ShouldDelegate() {
        givenUserWithTokensToDeactivate();

        whenDeactivatingTokens(List.of("token-1", "token-2"));

        thenTokensAreDeactivated("token-1", "token-2", "token-3");
    }

    @Test
    void deactivateTokens_WhenListIsNullOrEmpty_ShouldDoNothing() {
        givenUserWithSingleToken();

        whenDeactivatingTokens(null);
        whenDeactivatingTokens(List.of());

        thenTokenRemainsActive("token-1");
    }

    private void givenUserWithVariousTokens() {
        this.cachedUser = generateUser(RolUsuario.PADRE);
        createToken(this.cachedUser, "token-1", true);
        createToken(this.cachedUser, "", true);
        createToken(this.cachedUser, " ", true);
        createToken(this.cachedUser, "token-2", true);
        createToken(this.cachedUser, "token-3", false);
        entityManager.flush();
        entityManager.clear();
    }

    private void givenUserWithTokensToDeactivate() {
        this.cachedUser = generateUser(RolUsuario.PADRE);
        createToken(this.cachedUser, "token-1", true);
        createToken(this.cachedUser, "token-2", true);
        createToken(this.cachedUser, "token-3", true);
        entityManager.flush();
        entityManager.clear();
    }

    private void givenUserWithSingleToken() {
        this.cachedUser = generateUser(RolUsuario.PADRE);
        createToken(this.cachedUser, "token-1", true);
        entityManager.flush();
        entityManager.clear();
    }

    private List<String> whenFindingActiveTokens() {
        return repository.findActiveTokensByUserId(this.cachedUser.getId());
    }

    private void whenDeactivatingTokens(List<String> tokensToDeactivate) {
        repository.deactivateTokens(tokensToDeactivate);
        entityManager.flush();
        entityManager.clear();
    }

    private void thenOnlyValidActiveTokensAreReturned(List<String> tokens) {
        assertEquals(List.of("token-1", "token-2"), tokens);
    }

    private void thenTokensAreDeactivated(String token1, String token2, String activeToken) {
        DispositivoUsuarioEntity t1 = springDataRepository.findByFcmToken(token1).orElseThrow();
        assertFalse(t1.getActivo());

        DispositivoUsuarioEntity t2 = springDataRepository.findByFcmToken(token2).orElseThrow();
        assertFalse(t2.getActivo());

        DispositivoUsuarioEntity t3 = springDataRepository.findByFcmToken(activeToken).orElseThrow();
        assertTrue(t3.getActivo());
    }

    private void thenTokenRemainsActive(String token) {
        DispositivoUsuarioEntity t1 = springDataRepository.findByFcmToken(token).orElseThrow();
        assertTrue(t1.getActivo());
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
