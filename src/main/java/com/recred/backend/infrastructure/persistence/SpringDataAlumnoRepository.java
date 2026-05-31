package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataAlumnoRepository extends JpaRepository<StudentEntity, UUID> {

    @Query("""
            select student.user.id
            from StudentEntity student
            where student.id = :studentId
            """)
    Optional<UUID> findUserIdByStudentId(@Param("studentId") UUID studentId);
}
