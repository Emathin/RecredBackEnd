package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Billetera;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository {
    Optional<Billetera> findByStudentId(UUID studentId);
    void save(Billetera wallet);
    java.util.List<UUID> findAlumnoIdsWithLowBalance(java.math.BigDecimal threshold);
}
