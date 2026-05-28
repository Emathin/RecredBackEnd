package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.AlumnoEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConsumptionAlertStudentJpaRepository
        extends JpaRepository<AlumnoEntity, UUID> {

    @Query("""
            select distinct a
            from AlumnoEntity a
            join fetch a.usuario u
            left join fetch a.tutores t
            left join fetch t.usuario tu
            where a.activo = true
            and u.activo = true
            """)
    List<AlumnoEntity> findActiveStudentsWithUsersAndTutors();
}
