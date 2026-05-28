package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.StudentConsumptionPurchase;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StudentConsumptionRepository {
    List<StudentConsumptionPurchase> findPurchasesByStudentIdAndDateRange(
            UUID studentId,
            Collection<EstadoCompra> statuses,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );
}
