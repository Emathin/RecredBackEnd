package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.repositories.SchoolCalendarRepository;
import com.recred.backend.infrastructure.entities.CalendarioEscolarEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SchoolCalendarJpaRepository
        extends JpaRepository<CalendarioEscolarEntity, UUID>, SchoolCalendarRepository {

    @Override
    @Query("""
            select new com.recred.backend.domain.models.SchoolCalendarEvent(
                calendarEvent.fecha,
                calendarEvent.tipoDia,
                calendarEvent.descripcion
            )
            from CalendarioEscolarEntity calendarEvent
            where calendarEvent.colegioEntity.id = (
                select student.gradoEntity.colegioEntity.id
                from AlumnoEntity student
                where student.id = :studentId
            )
            and calendarEvent.fecha between :startDate and :endDate
            order by calendarEvent.fecha asc
            """)
    List<SchoolCalendarEvent> findByStudentIdAndDateRange(
            @Param("studentId") UUID studentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
