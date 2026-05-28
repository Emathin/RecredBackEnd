package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ConsumptionRequester;
import com.recred.backend.domain.repositories.ConsumptionAccessRepository;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserConsumptionAccessJpaRepository
        extends JpaRepository<UsuarioEntity, UUID>, ConsumptionAccessRepository {

    @Override
    @Query("""
            select new com.recred.backend.domain.models.ConsumptionRequester(
                user.id,
                user.rol,
                student.id
            )
            from UsuarioEntity user
            left join user.alumnoEntity student
            where user.idCognito = :cognitoSubject
            and user.activo = true
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
            join tutor.hijos student
            where tutor.usuario.id = :userId
            and tutor.activa = true
            and student.id = :studentId
            and student.activo = true
            """)
    long countTutorStudents(
            @Param("userId") UUID userId,
            @Param("studentId") UUID studentId
    );
}
