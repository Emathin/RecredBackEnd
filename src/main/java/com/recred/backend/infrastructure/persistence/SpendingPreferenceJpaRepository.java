package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.StudentBudgetEntity;
import com.recred.backend.domain.models.SpendingPeriodPreference;
import com.recred.backend.domain.repositories.SpendingPreferenceRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpendingPreferenceJpaRepository
        extends JpaRepository<StudentBudgetEntity, UUID>, SpendingPreferenceRepository {

    @Override
    @Query("""
            select new com.recred.backend.domain.models.SpendingPeriodPreference(
                p.period,
                p.limitAmount
            )
            from StudentBudgetEntity p
            where p.studentEntity.id = :studentId
            and p.active = true
            and p.startDate <= :date
            and (p.endDate is null or p.endDate >= :date)
            order by p.startDate desc
            """)
    List<SpendingPeriodPreference> findActivePreferences(
            @Param("studentId") UUID studentId,
            @Param("date") LocalDate date
    );
}
