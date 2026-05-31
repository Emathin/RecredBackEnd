package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.infrastructure.entities.StudentEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import com.recred.backend.infrastructure.entities.UserEntity;
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
        StudentEntity student = student("Juan", "Perez");
        TutorEntity activeTutor = tutor("Ana", "Perez", true, true);
        TutorEntity inactiveTutor = tutor("Luis", "Perez", false, true);
        TutorEntity inactiveUserTutor = tutor("Maria", "Perez", true, false);
        TutorEntity tutorWithoutUser = new TutorEntity();
        tutorWithoutUser.setActive(true);
        student.getTutors().add(activeTutor);
        student.getTutors().add(inactiveTutor);
        student.getTutors().add(inactiveUserTutor);
        student.getTutors().add(tutorWithoutUser);
        when(jpaRepository.findActiveStudentsWithUsersAndTutors()).thenReturn(List.of(student));

        List<ConsumptionAlertCandidate> candidates = repository.findActiveCandidates();

        assertEquals(1, candidates.size());
        assertEquals(student.getId(), candidates.get(0).studentId());
        assertEquals(student.getUser().getId(), candidates.get(0).studentUserId());
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
        StudentEntity student = student(null, " ");
        when(jpaRepository.findActiveStudentsWithUsersAndTutors()).thenReturn(List.of(student));

        List<ConsumptionAlertCandidate> candidates = repository.findActiveCandidates();

        assertEquals("el alumno", candidates.get(0).studentName());
    }

    private StudentEntity student(String firstName, String lastName) {
        StudentEntity student = new StudentEntity();
        student.setId(UUID.randomUUID());
        student.setUser(user(firstName, lastName, true));
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
        tutor.setActive(activeTutor);
        tutor.setUser(user(firstName, lastName, activeUser));
        return tutor;
    }

    private UserEntity user(String firstName, String lastName, boolean active) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setName(firstName);
        user.setSurname(lastName);
        user.setActive(active);
        return user;
    }
}
