package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.SchoolAssistantPurchase;
import java.util.List;
import java.util.UUID;

public interface SchoolAssistantPurchaseRepository {
    List<SchoolAssistantPurchase> findRecentByStudentId(UUID studentId, int limit);
}
