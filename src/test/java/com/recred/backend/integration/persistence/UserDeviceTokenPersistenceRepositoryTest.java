package com.recred.backend.integration.persistence;

import java.util.List;
import java.util.UUID;

import com.recred.backend.infrastructure.persistence.UserDeviceTokenJpaRepository;
import com.recred.backend.infrastructure.persistence.UserDeviceTokenPersistenceRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
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
}
