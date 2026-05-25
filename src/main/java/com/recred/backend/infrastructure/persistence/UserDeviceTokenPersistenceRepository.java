package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.repositories.UserDeviceTokenRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UserDeviceTokenPersistenceRepository implements UserDeviceTokenRepository {

    private final UserDeviceTokenJpaRepository userDeviceTokenJpaRepository;

    public UserDeviceTokenPersistenceRepository(
            UserDeviceTokenJpaRepository userDeviceTokenJpaRepository
    ) {
        this.userDeviceTokenJpaRepository = userDeviceTokenJpaRepository;
    }

    @Override
    public List<String> findActiveTokensByUserId(UUID userId) {
        return userDeviceTokenJpaRepository.findActiveTokensByUserId(userId)
                .stream()
                .filter(token -> token != null && !token.isBlank())
                .toList();
    }
}
