package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.PayoutScheduleStatus;
import com.recred.backend.infrastructure.entities.KiosqueroPayoutConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface KiosqueroPayoutConfigJpaRepository 
        extends JpaRepository<KiosqueroPayoutConfigEntity, UUID> {
        
    List<KiosqueroPayoutConfigEntity> findByEstadoAndProximaEjecucionLessThanEqual(
        PayoutScheduleStatus estado, 
        LocalDate date
    );
}
