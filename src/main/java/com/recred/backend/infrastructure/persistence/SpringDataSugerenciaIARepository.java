package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SpringDataSugerenciaIARepository extends JpaRepository<SugerenciaIAEntity, UUID> {
    List<SugerenciaIAEntity> findAllByAlumnoEntityIdOrderByCreadoEnDesc(UUID alumnoId);

    List<SugerenciaIAEntity> findAllByAlumnoEntityIdAndTipoOrderByCreadoEnDesc(
            UUID alumnoId, TipoSugerenciaIA tipo);

    boolean existsByAlumnoEntityIdAndProductoEntityIdAndTipoAndCreadoEnGreaterThanEqual(
            UUID alumnoId,
            UUID productoId,
            TipoSugerenciaIA tipo,
            LocalDateTime creadoEn
    );
}
