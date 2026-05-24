package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.PresupuestoAlumnoEntity;
import com.recred.backend.domain.models.PreferenciaPeriodoGasto;
import com.recred.backend.domain.repositories.PreferenciaGastoRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PreferenciaGastoJpaRepository
        extends JpaRepository<PresupuestoAlumnoEntity, UUID>, PreferenciaGastoRepository {

    @Override
    @Query("""
            select new com.recred.backend.domain.models.PreferenciaPeriodoGasto(
                p.periodo,
                p.montoLimite
            )
            from PresupuestoAlumnoEntity p
            where p.alumnoEntity.id = :alumnoId
            and p.activo = true
            and p.fechaInicio <= :fecha
            and (p.fechaFin is null or p.fechaFin >= :fecha)
            order by p.fechaInicio desc
            """)
    List<PreferenciaPeriodoGasto> buscarPreferenciasActivas(
            @Param("alumnoId") UUID alumnoId,
            @Param("fecha") LocalDate fecha
    );
}
