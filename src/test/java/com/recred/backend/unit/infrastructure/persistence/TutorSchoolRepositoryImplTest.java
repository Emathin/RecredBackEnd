package com.recred.backend.unit.infrastructure.persistence;

import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.TutorSchool;
import com.recred.backend.domain.models.TutorSchoolStatus;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import com.recred.backend.infrastructure.entities.TutorSchoolEntity;
import com.recred.backend.infrastructure.persistence.SpringDataColegioRepository;
import com.recred.backend.infrastructure.persistence.SpringDataTutorRepository;
import com.recred.backend.infrastructure.persistence.SpringDataTutorSchoolRepository;
import com.recred.backend.infrastructure.persistence.TutorSchoolRepositoryImpl;
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
class TutorSchoolRepositoryImplTest {

    @Mock
    private SpringDataTutorSchoolRepository repository;

    @Mock
    private SpringDataTutorRepository tutorRepository;

    @Mock
    private SpringDataColegioRepository schoolRepository;

    private TutorSchoolRepositoryImpl adapter;

    @BeforeEach
    void setUp() {
        adapter = new TutorSchoolRepositoryImpl(
                repository,
                tutorRepository,
                schoolRepository
        );
    }

    @Test
    void findByTutorIdAndSchoolIdShouldMapEntity() {
        UUID tutorId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        TutorSchoolEntity entity = tutorSchoolEntity(UUID.randomUUID(), tutorId, schoolId);
        when(repository.findByTutorAndSchool(tutorId, schoolId))
                .thenReturn(Optional.of(entity));

        Optional<TutorSchool> result = adapter.findByTutorIdAndSchoolId(tutorId, schoolId);

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.orElseThrow().getId());
        assertEquals(tutorId, result.orElseThrow().getTutorId());
        assertEquals(schoolId, result.orElseThrow().getSchool().getId());
    }

    @Test
    void existsByTutorIdAndSchoolIdAndStatusShouldDelegate() {
        UUID tutorId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        when(repository.existsByTutorAndSchoolAndStatus(
                tutorId,
                schoolId,
                TutorSchoolStatus.ACTIVE)).thenReturn(true);

        assertTrue(adapter.existsByTutorIdAndSchoolIdAndStatus(
                tutorId,
                schoolId,
                TutorSchoolStatus.ACTIVE
        ));
    }

    @Test
    void findActiveSchoolsByTutorIdShouldReturnSchoolsOrderedByRepository() {
        UUID tutorId = UUID.randomUUID();
        UUID firstSchoolId = UUID.randomUUID();
        UUID secondSchoolId = UUID.randomUUID();
        when(repository.findByTutorAndStatusOrderBySchoolNameAsc(
                tutorId,
                TutorSchoolStatus.ACTIVE))
                .thenReturn(List.of(
                        tutorSchoolEntity(UUID.randomUUID(), tutorId, firstSchoolId),
                        tutorSchoolEntity(UUID.randomUUID(), tutorId, secondSchoolId)
                ));

        List<Colegio> result = adapter.findActiveSchoolsByTutorId(tutorId);

        assertEquals(2, result.size());
        assertEquals(firstSchoolId, result.get(0).getId());
        assertEquals(secondSchoolId, result.get(1).getId());
    }

    @Test
    void saveShouldCreateEntityWithTutorAndSchoolReferences() {
        UUID tutorId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        TutorEntity tutor = tutorEntity(tutorId);
        ColegioEntity school = schoolEntity(schoolId);
        when(tutorRepository.getReferenceById(tutorId)).thenReturn(tutor);
        when(schoolRepository.getReferenceById(schoolId)).thenReturn(school);
        when(repository.save(any(TutorSchoolEntity.class))).thenAnswer(invocation -> {
            TutorSchoolEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(LocalDateTime.of(2026, 7, 1, 10, 0));
            return entity;
        });

        TutorSchool result = adapter.save(tutorSchool(null, tutorId, schoolId));

        ArgumentCaptor<TutorSchoolEntity> captor =
                ArgumentCaptor.forClass(TutorSchoolEntity.class);
        verify(repository).save(captor.capture());
        assertEquals(tutorId, captor.getValue().getTutor().getId());
        assertEquals(schoolId, captor.getValue().getSchool().getId());
        assertEquals(TutorSchoolStatus.ACTIVE, result.getStatus());
    }

    @Test
    void saveShouldUpdateExistingEntityWhenIdIsPresent() {
        UUID tutorSchoolId = UUID.randomUUID();
        TutorSchoolEntity existing = tutorSchoolEntity(
                tutorSchoolId,
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        when(repository.findById(tutorSchoolId)).thenReturn(Optional.of(existing));
        when(repository.save(any(TutorSchoolEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TutorSchool result = adapter.save(tutorSchool(
                tutorSchoolId,
                null,
                null
        ));

        assertEquals(tutorSchoolId, result.getId());
        assertEquals(TutorSchoolStatus.ACTIVE, result.getStatus());
    }

    private TutorSchool tutorSchool(UUID id, UUID tutorId, UUID schoolId) {
        TutorSchool tutorSchool = new TutorSchool();
        tutorSchool.setId(id);
        tutorSchool.setTutorId(tutorId);
        tutorSchool.setSchool(schoolId == null ? null : school(schoolId));
        tutorSchool.setStatus(TutorSchoolStatus.ACTIVE);
        return tutorSchool;
    }

    private Colegio school(UUID schoolId) {
        Colegio school = new Colegio();
        school.setId(schoolId);
        school.setNombre("Colegio");
        return school;
    }

    private TutorSchoolEntity tutorSchoolEntity(UUID id, UUID tutorId, UUID schoolId) {
        TutorSchoolEntity entity = new TutorSchoolEntity();
        entity.setId(id);
        entity.setTutor(tutorEntity(tutorId));
        entity.setSchool(schoolEntity(schoolId));
        entity.setStatus(TutorSchoolStatus.ACTIVE);
        entity.setCreatedAt(LocalDateTime.of(2026, 7, 1, 10, 0));
        entity.setUpdatedAt(LocalDateTime.of(2026, 7, 1, 11, 0));
        return entity;
    }

    private TutorEntity tutorEntity(UUID tutorId) {
        TutorEntity entity = new TutorEntity();
        entity.setId(tutorId);
        return entity;
    }

    private ColegioEntity schoolEntity(UUID schoolId) {
        ColegioEntity entity = new ColegioEntity();
        entity.setId(schoolId);
        entity.setNombre("Colegio");
        entity.setCue("CUE-" + schoolId);
        entity.setActivo(true);
        return entity;
    }
}
