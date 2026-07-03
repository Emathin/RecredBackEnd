package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.TutorSchoolStatus;
import com.recred.backend.infrastructure.entities.TutorSchoolEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataTutorSchoolRepository extends JpaRepository<TutorSchoolEntity, UUID> {

    @EntityGraph(attributePaths = {"school", "school.nivelEntity"})
    @Query("""
            select tutorSchool
            from TutorSchoolEntity tutorSchool
            where tutorSchool.tutor.id = :tutorId
              and tutorSchool.school.id = :schoolId
            """)
    Optional<TutorSchoolEntity> findByTutorAndSchool(
            @Param("tutorId") UUID tutorId,
            @Param("schoolId") UUID schoolId);

    @Query("""
            select count(tutorSchool) > 0
            from TutorSchoolEntity tutorSchool
            where tutorSchool.tutor.id = :tutorId
              and tutorSchool.school.id = :schoolId
              and tutorSchool.status = :status
            """)
    boolean existsByTutorAndSchoolAndStatus(
            @Param("tutorId") UUID tutorId,
            @Param("schoolId") UUID schoolId,
            @Param("status") TutorSchoolStatus status);

    @EntityGraph(attributePaths = {"school", "school.nivelEntity"})
    @Query("""
            select tutorSchool
            from TutorSchoolEntity tutorSchool
            where tutorSchool.tutor.id = :tutorId
              and tutorSchool.status = :status
            order by tutorSchool.school.nombre asc
            """)
    List<TutorSchoolEntity> findByTutorAndStatusOrderBySchoolNameAsc(
            @Param("tutorId") UUID tutorId,
            @Param("status") TutorSchoolStatus status);
}
