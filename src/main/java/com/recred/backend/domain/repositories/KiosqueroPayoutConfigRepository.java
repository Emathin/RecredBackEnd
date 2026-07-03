package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.KiosqueroPayoutConfig;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KiosqueroPayoutConfigRepository {
    KiosqueroPayoutConfig save(KiosqueroPayoutConfig config);
    Optional<KiosqueroPayoutConfig> findByKiosqueroId(UUID kiosqueroId);
    List<KiosqueroPayoutConfig> findActiveConfigsDueOnOrBefore(LocalDate date);
}
