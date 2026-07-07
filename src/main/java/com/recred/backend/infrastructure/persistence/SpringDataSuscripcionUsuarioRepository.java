package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.infrastructure.entities.SuscripcionUsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataSuscripcionUsuarioRepository
        extends JpaRepository<SuscripcionUsuarioEntity, UUID> {

    Optional<SuscripcionUsuarioEntity> findFirstByUsuarioEntityIdAndEstadoOrderByFechaInicioDesc(
            UUID usuarioId,
            EstadoSuscripcion estado
    );

    List<SuscripcionUsuarioEntity> findByUsuarioEntityIdOrderByFechaInicioDesc(UUID usuarioId);

    List<SuscripcionUsuarioEntity>
            findByEstadoAndFechaVencimientoLessThanEqualOrderByFechaVencimientoAsc(
                    EstadoSuscripcion estado,
                    LocalDateTime fechaVencimiento
            );

    List<SuscripcionUsuarioEntity>
            findByEstadoAndFechaVencimientoBetweenOrderByFechaVencimientoAsc(
                    EstadoSuscripcion estado,
                    LocalDateTime from,
                    LocalDateTime to
            );
}
