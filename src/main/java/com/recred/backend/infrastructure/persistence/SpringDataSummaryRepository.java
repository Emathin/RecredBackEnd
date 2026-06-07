package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.SummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataSummaryRepository extends JpaRepository<SummaryEntity, UUID> {
    @Query("SELECT s FROM SummaryEntity s WHERE s.user.id = :userId " +
            "ORDER BY s.endDate DESC LIMIT 1")
    Optional<SummaryEntity> findTopByUsuarioIdOrderByFechaHastaDesc(UUID userId);
}
