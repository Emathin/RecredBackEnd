package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.ConsumptionAlertTutor;
import com.recred.backend.domain.repositories.ConsumptionAlertStudentRepository;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
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

    private ConsumptionAlertCandidate mapCandidate(AlumnoEntity student) {
        UsuarioEntity user = student.getUsuario();
        return new ConsumptionAlertCandidate(
                student.getId(),
                user.getId(),
                fullName(user),
                tutors(student)
        );
    }

    private List<ConsumptionAlertTutor> tutors(AlumnoEntity student) {
        return student.getTutores()
                .stream()
                .filter(this::isActiveTutor)
                .map(this::mapTutor)
                .sorted(Comparator.comparing(ConsumptionAlertTutor::name))
                .toList();
    }

    private boolean isActiveTutor(TutorEntity tutor) {
        return Boolean.TRUE.equals(tutor.getActiva())
                && tutor.getUsuario() != null
                && Boolean.TRUE.equals(tutor.getUsuario().getActivo());
    }

    private ConsumptionAlertTutor mapTutor(TutorEntity tutor) {
        return new ConsumptionAlertTutor(
                tutor.getUsuario().getId(),
                fullName(tutor.getUsuario()),
                tutor.getUsuario().getPlan()
        );
    }

    private String fullName(UsuarioEntity user) {
        String fullName = (safeText(user.getNombre()) + " " + safeText(user.getApellido()))
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
