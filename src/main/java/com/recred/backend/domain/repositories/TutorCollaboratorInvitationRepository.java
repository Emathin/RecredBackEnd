package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.TutorCollaboratorInvitation;
import java.util.Optional;
import java.util.UUID;

public interface TutorCollaboratorInvitationRepository {
    void save(TutorCollaboratorInvitation invitation);
    Optional<TutorCollaboratorInvitation> findByTokenHash(String tokenHash);
    Optional<TutorCollaboratorInvitation> findById(UUID id);
}
