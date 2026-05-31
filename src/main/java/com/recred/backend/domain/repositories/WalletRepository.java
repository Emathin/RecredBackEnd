package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Wallet;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository {
    Optional<Wallet> findByStudentId(UUID studentId);
    void save(Wallet wallet);
}
