package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.repositories.SchoolCalendarRepository;
import com.recred.backend.infrastructure.entities.SchoolCalendarEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SchoolCalendarJpaRepository
        extends JpaRepository<SchoolCalendarEntity, UUID>, SchoolCalendarRepository {

    @Override
    @Query("""
            select new com.recred.backend.domain.models.SchoolCalendarEvent(
                calendarEvent.date,
                calendarEvent.dayType,
                calendarEvent.description
            )
            from SchoolCalendarEntity calendarEvent
            where calendarEvent.schoolEntity.id = (
                select student.gradeEntity.schoolEntity.id
                from StudentEntity student
                where student.id = :studentId
            )
            and calendarEvent.date between :startDate and :endDate
            order by calendarEvent.date asc
            """)
    List<SchoolCalendarEvent> findByStudentIdAndDateRange(
            @Param("studentId") UUID studentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
