package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ConsumptionRequester;
import com.recred.backend.domain.repositories.ConsumptionAccessRepository;
import com.recred.backend.infrastructure.entities.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserConsumptionAccessJpaRepository
        extends JpaRepository<UserEntity, UUID>, ConsumptionAccessRepository {

    @Override
    @Query("""
            select new com.recred.backend.domain.models.ConsumptionRequester(
                user.id,
                user.rol,
                student.id
            )
            from UserEntity user
            left join user.studentEntity student
            where user.idCognito = :cognitoSubject
            and user.active = true
            """)
    Optional<ConsumptionRequester> findRequesterByCognitoSubject(
            @Param("cognitoSubject") String cognitoSubject
    );

    @Override
    default boolean tutorCanAccessStudent(UUID userId, UUID studentId) {
        return countTutorStudents(userId, studentId) > 0;
    }

    @Query("""
            select count(student.id)
            from TutorEntity tutor
            join tutor.children student
            where tutor.user.id = :userId
            and tutor.active = true
            and student.id = :studentId
            and student.active = true
            """)
    long countTutorStudents(
            @Param("userId") UUID userId,
            @Param("studentId") UUID studentId
    );
}
