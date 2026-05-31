package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataWalletRepository extends JpaRepository<WalletEntity, UUID> {
    Optional<WalletEntity> findByAlumnoEntityId(UUID studentId);
}
