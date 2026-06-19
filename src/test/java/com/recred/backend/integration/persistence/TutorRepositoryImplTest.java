package com.recred.backend.integration.persistence;

import com.recred.backend.infrastructure.persistence.*;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TutorRepositoryImplTest {

    @Mock
    private SpringDataTutorRepository springDataTutorRepository;

    @Mock
    private SpringDataAlumnoRepository springDataAlumnoRepository;

    private TutorRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new TutorRepositoryImpl(
                springDataTutorRepository,
                springDataAlumnoRepository
        );
    }

    @Test
    void findMethods_ShouldMapEntitiesToDomain() {
        UUID tutorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        TutorEntity entity = tutorEntity(tutorId);
        when(springDataTutorRepository.findById(tutorId)).thenReturn(Optional.of(entity));
        when(springDataTutorRepository.findByUsuarioId(userId)).thenReturn(Optional.of(entity));
        when(springDataTutorRepository.findByUsuarioIdCognito("subject"))
                .thenReturn(Optional.of(entity));
        when(springDataTutorRepository.findByAlumnoId(studentId)).thenReturn(List.of(entity));
        when(springDataTutorRepository.findAll()).thenReturn(List.of(entity));

        assertEquals(tutorId, repository.findById(tutorId).orElseThrow().getId());
        assertEquals(tutorId, repository.findByUsuarioId(userId).orElseThrow().getId());
        assertEquals(tutorId,
                repository.findByUsuarioIdCognito("subject").orElseThrow().getId());
        assertEquals(1, repository.findByAlumnoId(studentId).size());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void save_WhenTutorDoesNotExist_ShouldFail() {
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        when(springDataTutorRepository.findById(tutor.getId())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> repository.save(tutor));
    }

    @Test
    void save_ShouldReplaceChildrenAndIgnoreChildWithoutId() {
        UUID tutorId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        TutorEntity existing = tutorEntity(tutorId);
        existing.addHijo(new AlumnoEntity());
        AlumnoEntity childReference = new AlumnoEntity();
        childReference.setId(childId);
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        Alumno child = new Alumno();
        child.setId(childId);
        tutor.setHijos(Set.of(child, new Alumno()));
        when(springDataTutorRepository.findById(tutorId)).thenReturn(Optional.of(existing));
        when(springDataAlumnoRepository.getReferenceById(childId)).thenReturn(childReference);
        when(springDataTutorRepository.save(existing)).thenReturn(existing);

        Tutor result = repository.save(tutor);

        assertEquals(tutorId, result.getId());
        assertEquals(1, existing.getHijos().size());
        assertTrue(existing.getHijos().contains(childReference));
        verify(springDataTutorRepository).save(existing);
    }

    private TutorEntity tutorEntity(UUID tutorId) {
        TutorEntity entity = new TutorEntity();
        entity.setId(tutorId);
        entity.setActiva(true);
        return entity;
    }
}
