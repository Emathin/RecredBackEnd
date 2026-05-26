package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsumptionAlertStudentPersistenceRepositoryTest {

    @Test
    void mapActiveStudentsWithActiveTutors() {
        ConsumptionAlertStudentJpaRepository jpaRepository = mock(
                ConsumptionAlertStudentJpaRepository.class);
        ConsumptionAlertStudentPersistenceRepository repository =
                new ConsumptionAlertStudentPersistenceRepository(jpaRepository);
        AlumnoEntity student = student("Juan", "Perez");
        TutorEntity activeTutor = tutor("Ana", "Perez", true, true);
        TutorEntity inactiveTutor = tutor("Luis", "Perez", false, true);
        TutorEntity inactiveUserTutor = tutor("Maria", "Perez", true, false);
        TutorEntity tutorWithoutUser = new TutorEntity();
        tutorWithoutUser.setActiva(true);
        student.getTutores().add(activeTutor);
        student.getTutores().add(inactiveTutor);
        student.getTutores().add(inactiveUserTutor);
        student.getTutores().add(tutorWithoutUser);
        when(jpaRepository.findActiveStudentsWithUsersAndTutors()).thenReturn(List.of(student));

        List<ConsumptionAlertCandidate> candidates = repository.findActiveCandidates();

        assertEquals(1, candidates.size());
        assertEquals(student.getId(), candidates.get(0).studentId());
        assertEquals(student.getUsuario().getId(), candidates.get(0).studentUserId());
        assertEquals("Juan Perez", candidates.get(0).studentName());
        assertEquals(1, candidates.get(0).tutors().size());
        assertEquals("Ana Perez", candidates.get(0).tutors().get(0).name());
    }

    @Test
    void useDefaultNameWhenStudentUserNameIsBlank() {
        ConsumptionAlertStudentJpaRepository jpaRepository = mock(
                ConsumptionAlertStudentJpaRepository.class);
        ConsumptionAlertStudentPersistenceRepository repository =
                new ConsumptionAlertStudentPersistenceRepository(jpaRepository);
        AlumnoEntity student = student(null, " ");
        when(jpaRepository.findActiveStudentsWithUsersAndTutors()).thenReturn(List.of(student));

        List<ConsumptionAlertCandidate> candidates = repository.findActiveCandidates();

        assertEquals("el alumno", candidates.get(0).studentName());
    }

    private AlumnoEntity student(String firstName, String lastName) {
        AlumnoEntity student = new AlumnoEntity();
        student.setId(UUID.randomUUID());
        student.setUsuario(user(firstName, lastName, true));
        return student;
    }

    private TutorEntity tutor(
            String firstName,
            String lastName,
            boolean activeTutor,
            boolean activeUser
    ) {
        TutorEntity tutor = new TutorEntity();
        tutor.setId(UUID.randomUUID());
        tutor.setActiva(activeTutor);
        tutor.setUsuario(user(firstName, lastName, activeUser));
        return tutor;
    }

    private UsuarioEntity user(String firstName, String lastName, boolean active) {
        UsuarioEntity user = new UsuarioEntity();
        user.setId(UUID.randomUUID());
        user.setNombre(firstName);
        user.setApellido(lastName);
        user.setActivo(active);
        return user;
    }
}
