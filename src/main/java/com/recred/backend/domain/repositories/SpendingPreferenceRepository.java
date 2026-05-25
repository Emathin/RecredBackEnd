package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.SpendingPeriodPreference;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SpendingPreferenceRepository {
    List<SpendingPeriodPreference> findActivePreferences(UUID studentId, LocalDate date);
}
