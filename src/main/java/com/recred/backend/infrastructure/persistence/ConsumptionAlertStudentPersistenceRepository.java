package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.ConsumptionAlertTutor;
import com.recred.backend.domain.repositories.ConsumptionAlertStudentRepository;
import com.recred.backend.infrastructure.entities.StudentEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import com.recred.backend.infrastructure.entities.UserEntity;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ConsumptionAlertStudentPersistenceRepository
        implements ConsumptionAlertStudentRepository {

    private static final String DEFAULT_STUDENT_NAME = "el alumno";

    private final ConsumptionAlertStudentJpaRepository studentJpaRepository;

    public ConsumptionAlertStudentPersistenceRepository(
            ConsumptionAlertStudentJpaRepository studentJpaRepository
    ) {
        this.studentJpaRepository = studentJpaRepository;
    }

    @Override
    public List<ConsumptionAlertCandidate> findActiveCandidates() {
        return studentJpaRepository.findActiveStudentsWithUsersAndTutors()
                .stream()
                .map(this::mapCandidate)
                .toList();
    }

    private ConsumptionAlertCandidate mapCandidate(StudentEntity student) {
        UserEntity user = student.getUser();
        return new ConsumptionAlertCandidate(
                student.getId(),
                user.getId(),
                fullName(user),
                tutors(student)
        );
    }

    private List<ConsumptionAlertTutor> tutors(StudentEntity student) {
        return student.getTutors()
                .stream()
                .filter(this::isActiveTutor)
                .map(this::mapTutor)
                .sorted(Comparator.comparing(ConsumptionAlertTutor::name))
                .toList();
    }

    private boolean isActiveTutor(TutorEntity tutor) {
        return Boolean.TRUE.equals(tutor.getActive())
                && tutor.getUser() != null
                && Boolean.TRUE.equals(tutor.getUser().getActive());
    }

    private ConsumptionAlertTutor mapTutor(TutorEntity tutor) {
        return new ConsumptionAlertTutor(
                tutor.getUser().getId(),
                fullName(tutor.getUser())
        );
    }

    private String fullName(UserEntity user) {
        String fullName = (safeText(user.getName()) + " " + safeText(user.getSurname()))
                .trim();
        if (fullName.isBlank()) {
            return DEFAULT_STUDENT_NAME;
        }
        return fullName;
    }

    private String safeText(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }
}
