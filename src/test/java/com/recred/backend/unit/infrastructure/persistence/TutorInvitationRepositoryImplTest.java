package com.recred.backend.unit.infrastructure.persistence;

import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.TutorInvitation;
import com.recred.backend.domain.models.TutorInvitationStatus;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.TutorInvitationEntity;
import com.recred.backend.infrastructure.persistence.SpringDataColegioRepository;
import com.recred.backend.infrastructure.persistence.SpringDataTutorInvitationRepository;
import com.recred.backend.infrastructure.persistence.TutorInvitationRepositoryImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TutorInvitationRepositoryImplTest {

    @Mock
    private SpringDataTutorInvitationRepository repository;

    @Mock
    private SpringDataColegioRepository schoolRepository;

    private TutorInvitationRepositoryImpl adapter;

    @BeforeEach
    void setUp() {
        adapter = new TutorInvitationRepositoryImpl(repository, schoolRepository);
    }

    @Test
    void findByTokenHashShouldMapEntityToDomain() {
        UUID invitationId = UUID.randomUUID();
        when(repository.findByTokenHash("hash"))
                .thenReturn(Optional.of(entity(invitationId, schoolEntity(UUID.randomUUID()))));

        Optional<TutorInvitation> result = adapter.findByTokenHash("hash");

        assertTrue(result.isPresent());
        assertEquals(invitationId, result.orElseThrow().getId());
        assertEquals("tutor@example.com", result.orElseThrow().getEmail());
    }

    @Test
    void findReusableShouldRequestPendingAndExpiredStatuses() {
        UUID schoolId = UUID.randomUUID();
        TutorInvitationEntity entity = entity(UUID.randomUUID(), schoolEntity(schoolId));
        when(repository.findReusableInvitations(
                schoolId,
                "tutor@example.com",
                List.of(TutorInvitationStatus.PENDING, TutorInvitationStatus.EXPIRED)))
                .thenReturn(List.of(entity));

        Optional<TutorInvitation> result = adapter.findReusableBySchoolIdAndEmail(
                schoolId,
                "tutor@example.com"
        );

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.orElseThrow().getId());
    }

    @Test
    void saveShouldCreateEntityAndAttachSchoolReference() {
        UUID schoolId = UUID.randomUUID();
        ColegioEntity school = schoolEntity(schoolId);
        TutorInvitation invitation = invitation(null, schoolId);
        when(schoolRepository.getReferenceById(schoolId)).thenReturn(school);
        when(repository.save(any(TutorInvitationEntity.class))).thenAnswer(invocation -> {
            TutorInvitationEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(LocalDateTime.of(2026, 7, 1, 9, 0));
            return entity;
        });

        TutorInvitation result = adapter.save(invitation);

        ArgumentCaptor<TutorInvitationEntity> captor =
                ArgumentCaptor.forClass(TutorInvitationEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("tutor@example.com", captor.getValue().getEmail());
        assertEquals(schoolId, captor.getValue().getSchool().getId());
        assertEquals(TutorInvitationStatus.PENDING, result.getStatus());
        assertEquals(schoolId, result.getSchool().getId());
    }

    @Test
    void saveShouldUpdateExistingEntityWhenInvitationHasId() {
        UUID invitationId = UUID.randomUUID();
        TutorInvitationEntity existing = entity(invitationId, schoolEntity(UUID.randomUUID()));
        TutorInvitation invitation = invitation(invitationId, null);
        when(repository.findById(invitationId)).thenReturn(Optional.of(existing));
        when(repository.save(any(TutorInvitationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TutorInvitation result = adapter.save(invitation);

        assertEquals(invitationId, result.getId());
        assertEquals("tutor@example.com", existing.getEmail());
    }

    @Test
    void existsBySchoolIdAndEmailAndStatusShouldDelegate() {
        UUID schoolId = UUID.randomUUID();
        when(repository.existsBySchoolAndEmailAndStatus(
                schoolId,
                "tutor@example.com",
                TutorInvitationStatus.PENDING)).thenReturn(true);

        assertTrue(adapter.existsBySchoolIdAndEmailAndStatus(
                schoolId,
                "tutor@example.com",
                TutorInvitationStatus.PENDING
        ));
    }

    private TutorInvitation invitation(UUID invitationId, UUID schoolId) {
        TutorInvitation invitation = new TutorInvitation();
        invitation.setId(invitationId);
        invitation.setSchool(schoolId == null ? null : school(schoolId));
        invitation.setEmail("tutor@example.com");
        invitation.setFirstName("Ana");
        invitation.setLastName("Gomez");
        invitation.setPhone("1122334455");
        invitation.setTokenHash("hash");
        invitation.setStatus(TutorInvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.of(2026, 7, 8, 9, 0));
        invitation.setUsedAt(null);
        return invitation;
    }

    private Colegio school(UUID schoolId) {
        Colegio school = new Colegio();
        school.setId(schoolId);
        school.setNombre("Colegio");
        return school;
    }

    private TutorInvitationEntity entity(UUID invitationId, ColegioEntity school) {
        TutorInvitationEntity entity = new TutorInvitationEntity();
        entity.setId(invitationId);
        entity.setSchool(school);
        entity.setEmail("tutor@example.com");
        entity.setFirstName("Ana");
        entity.setLastName("Gomez");
        entity.setPhone("1122334455");
        entity.setTokenHash("hash");
        entity.setStatus(TutorInvitationStatus.PENDING);
        entity.setExpiresAt(LocalDateTime.of(2026, 7, 8, 9, 0));
        entity.setCreatedAt(LocalDateTime.of(2026, 7, 1, 9, 0));
        return entity;
    }

    private ColegioEntity schoolEntity(UUID schoolId) {
        ColegioEntity school = new ColegioEntity();
        school.setId(schoolId);
        school.setNombre("Colegio");
        school.setCue("CUE-" + schoolId);
        school.setActivo(true);
        return school;
    }
}
