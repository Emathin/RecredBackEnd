package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import com.recred.backend.domain.repositories.TutorInvitationRepository;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.TutorInvitationEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TutorInvitationRepositoryImpl implements TutorInvitationRepository {

    private final SpringDataTutorInvitationRepository repository;
    private final SpringDataColegioRepository schoolRepository;

    public TutorInvitationRepositoryImpl(
            SpringDataTutorInvitationRepository repository,
            SpringDataColegioRepository schoolRepository) {
        this.repository = repository;
        this.schoolRepository = schoolRepository;
    }

    @Override
    public Optional<TutorInvitation> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash)
                .map(TutorInvitationEntity::toDomain);
    }

    @Override
    public Optional<TutorInvitation> findReusableBySchoolIdAndEmail(
            UUID schoolId,
            String email) {
        return repository.findReusableInvitations(
                        schoolId,
                        email,
                        List.of(TutorInvitationStatus.PENDING, TutorInvitationStatus.EXPIRED))
                .stream()
                .findFirst()
                .map(TutorInvitationEntity::toDomain);
    }

    @Override
    public TutorInvitation save(TutorInvitation invitation) {
        TutorInvitationEntity entity = invitation.getId() == null
                ? new TutorInvitationEntity()
                : repository.findById(invitation.getId()).orElseGet(TutorInvitationEntity::new);

        entity.setId(invitation.getId());
        entity.setEmail(invitation.getEmail());
        entity.setFirstName(invitation.getFirstName());
        entity.setLastName(invitation.getLastName());
        entity.setPhone(invitation.getPhone());
        entity.setTokenHash(invitation.getTokenHash());
        entity.setStatus(invitation.getStatus());
        entity.setExpiresAt(invitation.getExpiresAt());
        entity.setUsedAt(invitation.getUsedAt());

        if (invitation.getSchool() != null && invitation.getSchool().getId() != null) {
            ColegioEntity school = schoolRepository.getReferenceById(
                    invitation.getSchool().getId());
            entity.setSchool(school);
        }

        return repository.save(entity).toDomain();
    }

    @Override
    public boolean existsBySchoolIdAndEmailAndStatus(
            UUID schoolId,
            String email,
            TutorInvitationStatus status) {
        return repository.existsBySchoolAndEmailAndStatus(schoolId, email, status);
    }
}
