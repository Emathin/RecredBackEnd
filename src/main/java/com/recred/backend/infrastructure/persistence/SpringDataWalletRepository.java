package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.BilleteraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataWalletRepository extends JpaRepository<BilleteraEntity, UUID> {
    Optional<BilleteraEntity> findByAlumnoEntityId(UUID studentId);
}
