package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.models.DailyInventorySnapshot;
import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.CierreDiarioBuffetEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DailyBuffetCloseRepositoryImpl implements DailyBuffetCloseRepository {

    private final SpringDataDailyBuffetCloseRepository springDataRepository;

    public DailyBuffetCloseRepositoryImpl(
            SpringDataDailyBuffetCloseRepository springDataRepository
    ) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DailyBuffetClose> findByBuffetAndDate(UUID buffetId, LocalDate date) {
        return springDataRepository.findByBuffetEntityIdAndFecha(buffetId, date)
                .map(CierreDiarioBuffetEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyBuffetClose> findByBuffetAndDateRange(
            UUID buffetId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return springDataRepository.findByBuffetAndDateRange(buffetId, startDate, endDate)
                .stream()
                .map(CierreDiarioBuffetEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public DailyBuffetClose save(DailyBuffetClose close) {
        CierreDiarioBuffetEntity entity = new CierreDiarioBuffetEntity();
        entity.setId(close.id());
        entity.setBuffetEntity(buffetReference(close.buffetId()));
        entity.setFecha(close.date());
        entity.setPedidosVencidos(close.expiredPurchases());
        entity.setReservasLiberadas(close.releasedReservations());
        entity.setCreditosDevueltos(close.refundedCredits());
        return springDataRepository.save(entity).toDomain();
    }

    @Override
    @Transactional
    public DailyBuffetClose saveWithInventorySnapshot(
            DailyBuffetClose close,
            List<DailyInventorySnapshot> inventorySnapshot
    ) {
        CierreDiarioBuffetEntity entity = new CierreDiarioBuffetEntity();
        entity.setId(close.id());
        entity.setBuffetEntity(buffetReference(close.buffetId()));
        entity.setFecha(close.date());
        entity.setPedidosVencidos(close.expiredPurchases());
        entity.setReservasLiberadas(close.releasedReservations());
        entity.setCreditosDevueltos(close.refundedCredits());
        entity.setInventarioSnapshotFromDomain(inventorySnapshot);
        return springDataRepository.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<List<DailyInventorySnapshot>> findInventorySnapshotByBuffetAndDate(
            UUID buffetId,
            LocalDate date
    ) {
        return springDataRepository.findWithInventorySnapshotByBuffetAndDate(buffetId, date)
                .map(CierreDiarioBuffetEntity::inventarioSnapshotToDomain)
                .filter(snapshot -> !snapshot.isEmpty());
    }

    private BuffetEntity buffetReference(UUID buffetId) {
        BuffetEntity buffet = new BuffetEntity();
        buffet.setId(buffetId);
        return buffet;
    }
}
