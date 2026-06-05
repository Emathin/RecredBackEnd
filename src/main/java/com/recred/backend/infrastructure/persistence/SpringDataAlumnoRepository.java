package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.AlumnoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataAlumnoRepository extends JpaRepository<AlumnoEntity, UUID> {

    @Query("""
            select student.usuario.id
            from AlumnoEntity student
            where student.id = :studentId
            """)
    Optional<UUID> findUserIdByStudentId(@Param("studentId") UUID studentId);

    @Query("""
            select student.id
            from AlumnoEntity student
            where student.usuario.id = :userId
            """)
    Optional<UUID> findStudentIdByUserId(@Param("userId") UUID userId);

    @Query("""
                select s.usuario.id
                from AlumnoEntity s
                where s.id = :alumnoId
                """)
    UUID findUserId(UUID alumnoId);
}
