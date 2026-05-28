package com.recred.backend.domain.repositories;

import java.util.List;
import java.util.UUID;

public interface UserDeviceTokenRepository {
    List<String> findActiveTokensByUserId(UUID userId);
}
