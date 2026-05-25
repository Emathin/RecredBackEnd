package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.PresupuestoAlumnoEntity;
import com.recred.backend.domain.models.SpendingPeriodPreference;
import com.recred.backend.domain.repositories.SpendingPreferenceRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpendingPreferenceJpaRepository
        extends JpaRepository<PresupuestoAlumnoEntity, UUID>, SpendingPreferenceRepository {

    @Override
    @Query("""
            select new com.recred.backend.domain.models.SpendingPeriodPreference(
                p.periodo,
                p.montoLimite
            )
            from PresupuestoAlumnoEntity p
            where p.alumnoEntity.id = :studentId
            and p.activo = true
            and p.fechaInicio <= :date
            and (p.fechaFin is null or p.fechaFin >= :date)
            order by p.fechaInicio desc
            """)
    List<SpendingPeriodPreference> findActivePreferences(
            @Param("studentId") UUID studentId,
            @Param("date") LocalDate date
    );
}
