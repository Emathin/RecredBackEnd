package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;

import java.util.Optional;
import java.util.UUID;

public interface TutorInvitationRepository {

    Optional<TutorInvitation> findByTokenHash(String tokenHash);

    Optional<TutorInvitation> findReusableBySchoolIdAndEmail(UUID schoolId, String email);

    TutorInvitation save(TutorInvitation invitation);

    boolean existsBySchoolIdAndEmailAndStatus(
            UUID schoolId,
            String email,
            TutorInvitationStatus status);
}
