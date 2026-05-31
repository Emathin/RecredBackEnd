package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.StudentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConsumptionAlertStudentJpaRepository
        extends JpaRepository<StudentEntity, UUID> {

    @Query("""
            select distinct a
            from StudentEntity a
            join fetch a.user u
            left join fetch a.tutors t
            left join fetch t.user tu
            where a.active = true
            and u.active = true
            """)
    List<StudentEntity> findActiveStudentsWithUsersAndTutors();
}
