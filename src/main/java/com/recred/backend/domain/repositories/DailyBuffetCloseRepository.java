package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.DailyBuffetClose;
import com.recred.backend.domain.models.DailyInventorySnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyBuffetCloseRepository {
    Optional<DailyBuffetClose> findByBuffetAndDate(UUID buffetId, LocalDate date);

    List<DailyBuffetClose> findByBuffetAndDateRange(
            UUID buffetId,
            LocalDate startDate,
            LocalDate endDate);

    DailyBuffetClose save(DailyBuffetClose close);

    DailyBuffetClose saveWithInventorySnapshot(
            DailyBuffetClose close,
            List<DailyInventorySnapshot> inventorySnapshot);

    Optional<List<DailyInventorySnapshot>> findInventorySnapshotByBuffetAndDate(
            UUID buffetId,
            LocalDate date);
}
