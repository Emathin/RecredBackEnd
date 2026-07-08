package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.TutorInvitationStatus;
import com.recred.backend.infrastructure.entities.TutorInvitationEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataTutorInvitationRepository
        extends JpaRepository<TutorInvitationEntity, UUID> {

    @EntityGraph(attributePaths = {"school", "school.nivelEntity"})
    Optional<TutorInvitationEntity> findByTokenHash(String tokenHash);

    @Query("""
            select count(invitation) > 0
            from TutorInvitationEntity invitation
            where invitation.school.id = :schoolId
              and invitation.email = :email
              and invitation.status = :status
            """)
    boolean existsBySchoolAndEmailAndStatus(
            @Param("schoolId") UUID schoolId,
            @Param("email") String email,
            @Param("status") TutorInvitationStatus status);

    @EntityGraph(attributePaths = {"school", "school.nivelEntity"})
    @Query("""
            select invitation
            from TutorInvitationEntity invitation
            where invitation.school.id = :schoolId
              and invitation.email = :email
              and invitation.status in :statuses
            order by invitation.createdAt desc
            """)
    List<TutorInvitationEntity> findReusableInvitations(
            @Param("schoolId") UUID schoolId,
            @Param("email") String email,
            @Param("statuses") Collection<TutorInvitationStatus> statuses);
}
