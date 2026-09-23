package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.TutorCollaboratorInvitation;
import com.recred.backend.domain.repositories.TutorCollaboratorInvitationRepository;
import com.recred.backend.infrastructure.entities.TutorCollaboratorInvitationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TutorCollaboratorInvitationRepositoryImpl implements TutorCollaboratorInvitationRepository {

    private final JpaTutorCollaboratorInvitationRepository jpaRepository;

    @Override
    public void save(TutorCollaboratorInvitation invitation) {
        TutorCollaboratorInvitationEntity entity = TutorCollaboratorInvitationEntity.fromDomain(invitation);
        jpaRepository.save(entity);
        invitation.setId(entity.getId());
    }

    @Override
    public Optional<TutorCollaboratorInvitation> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash)
                .map(TutorCollaboratorInvitationEntity::toDomain);
    }

    @Override
    public Optional<TutorCollaboratorInvitation> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(TutorCollaboratorInvitationEntity::toDomain);
    }
}
