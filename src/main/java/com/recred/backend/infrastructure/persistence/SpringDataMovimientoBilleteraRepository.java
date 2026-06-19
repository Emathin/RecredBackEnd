package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.infrastructure.entities.MovimientoBilleteraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataMovimientoBilleteraRepository
        extends JpaRepository<MovimientoBilleteraEntity, UUID> {

    Optional<MovimientoBilleteraEntity> findFirstByBilleteraEntityIdAndTipoOrderByCreadoEnDesc(
            UUID billeteraId,
            TipoMovimientoBilletera tipo);

    @Query("""
            select movement
            from MovimientoBilleteraEntity movement
            where movement.billeteraEntity.alumnoEntity.id = :studentId
            and movement.creadoEn between :from and :to
            order by movement.creadoEn desc
            """)
    List<MovimientoBilleteraEntity> findByStudentIdAndDateRange(
            @Param("studentId") UUID studentId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
