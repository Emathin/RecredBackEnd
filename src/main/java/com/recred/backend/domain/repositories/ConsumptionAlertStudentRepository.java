package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import java.util.List;

public interface ConsumptionAlertStudentRepository {
    List<ConsumptionAlertCandidate> findActiveCandidates();
}
