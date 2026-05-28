package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.SchoolAssistantPayment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SchoolAssistantPaymentRepository {
    List<SchoolAssistantPayment> findRecentByStudentId(UUID studentId, int limit);

    List<SchoolAssistantPayment> findByStudentIdAndDateRange(
            UUID studentId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            int limit
    );
}
