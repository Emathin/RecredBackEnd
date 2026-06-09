package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.BilleteraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataWalletRepository extends JpaRepository<BilleteraEntity, UUID> {
    Optional<BilleteraEntity> findByAlumnoEntityId(UUID studentId);

    @org.springframework.data.jpa.repository.Query(
            "SELECT b.alumnoEntity.id FROM BilleteraEntity b "
            + "WHERE b.saldoCreditos < :threshold AND b.alumnoEntity.activo = true")
    java.util.List<UUID> findAlumnoIdsWithLowBalance(
            @org.springframework.data.repository.query.Param("threshold") 
            java.math.BigDecimal threshold);
}
