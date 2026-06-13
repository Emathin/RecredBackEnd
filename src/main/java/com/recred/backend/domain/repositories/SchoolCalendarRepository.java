package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.SchoolCalendarEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolCalendarRepository {
    List<SchoolCalendarEvent> findByStudentIdAndDateRange(
            UUID studentId,
            LocalDate from,
            LocalDate to
    );

    Optional<SchoolCalendarEvent> findByColegioIdAndFecha(
            UUID colegioId,
            LocalDate fecha
    );
}
