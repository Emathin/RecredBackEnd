package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID id);

    User save(User newUser);

    Optional<User> findByIdCognito(String sub);
}
