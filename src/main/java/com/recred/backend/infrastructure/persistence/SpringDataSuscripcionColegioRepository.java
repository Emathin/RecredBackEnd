package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.infrastructure.entities.SuscripcionColegioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataSuscripcionColegioRepository
        extends JpaRepository<SuscripcionColegioEntity, UUID> {

    Optional<SuscripcionColegioEntity> findFirstByColegioEntityIdAndEstadoOrderByFechaInicioDesc(
            UUID colegioId,
            EstadoSuscripcion estado
    );

    List<SuscripcionColegioEntity> findByColegioEntityIdOrderByFechaInicioDesc(UUID colegioId);
}
