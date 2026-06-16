package com.recred.backend.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDeviceTokenPersistenceRepositoryTest {

    @Test
    void returnOnlyNonBlankActiveTokens() {
        UUID userId = UUID.randomUUID();
        UserDeviceTokenJpaRepository jpaRepository = mock(UserDeviceTokenJpaRepository.class);
        UserDeviceTokenPersistenceRepository repository =
                new UserDeviceTokenPersistenceRepository(jpaRepository);
        when(jpaRepository.findActiveTokensByUserId(userId))
                .thenReturn(asList("token-1", null, "", " ", "token-2"));

        List<String> tokens = repository.findActiveTokensByUserId(userId);

        assertEquals(List.of("token-1", "token-2"), tokens);
    }

    @Test
    void deactivateTokens_WhenListHasValues_ShouldDelegate() {
        UserDeviceTokenJpaRepository jpaRepository = mock(UserDeviceTokenJpaRepository.class);
        UserDeviceTokenPersistenceRepository repository =
                new UserDeviceTokenPersistenceRepository(jpaRepository);
        List<String> tokens = List.of("token-1", "token-2");

        repository.deactivateTokens(tokens);

        verify(jpaRepository).deactivateTokens(tokens);
    }

    @Test
    void deactivateTokens_WhenListIsNullOrEmpty_ShouldDoNothing() {
        UserDeviceTokenJpaRepository jpaRepository = mock(UserDeviceTokenJpaRepository.class);
        UserDeviceTokenPersistenceRepository repository =
                new UserDeviceTokenPersistenceRepository(jpaRepository);

        repository.deactivateTokens(null);
        repository.deactivateTokens(List.of());

        verify(jpaRepository, never()).deactivateTokens(asList());
    }
}
