package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.MovimientoBilleteraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.recred.backend.domain.models.TipoMovimientoBilletera;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataMovimientoBilleteraRepository 
        extends JpaRepository<MovimientoBilleteraEntity, UUID> {
    
    Optional<MovimientoBilleteraEntity> findFirstByBilleteraEntityIdAndTipoOrderByCreadoEnDesc(
            UUID billeteraId, 
            TipoMovimientoBilletera tipo);
}
