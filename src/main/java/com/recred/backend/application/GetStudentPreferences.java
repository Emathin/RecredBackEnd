package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GetStudentPreferences {

    private final SugerenciaIARepository sugerenciaIARepository;
    private final AlumnoRepository alumnoRepository;
    private final TutorRepository tutorRepository;

    public GetStudentPreferences(
            SugerenciaIARepository sugerenciaIARepository,
            AlumnoRepository alumnoRepository,
            TutorRepository tutorRepository) {
        this.sugerenciaIARepository = sugerenciaIARepository;
        this.alumnoRepository = alumnoRepository;
        this.tutorRepository = tutorRepository;
    }

    public List<SugerenciaIAEntity> execute(
            UUID alumnoId, boolean onlyLast, TipoSugerenciaIA tipo) {
        List<SugerenciaIAEntity> preferences;

        if (tipo != null) {
            preferences = sugerenciaIARepository.findByAlumnoIdAndTipo(alumnoId, tipo);
        } else {
            preferences = sugerenciaIARepository.findByAlumnoId(alumnoId);
        }
        
        if (onlyLast && !preferences.isEmpty()) {
            return List.of(preferences.get(0));
        }
        
        return preferences;
    }

    @Transactional
    public List<StudentPreferenceResult> executeByUserId(
            UUID userId,
            boolean onlyLast,
            TipoSugerenciaIA tipo
    ) {
        Optional<Tutor> tutor = tutorRepository.findByUsuarioId(userId);
        if (tutor.isPresent()) {
            return executeForParent(tutor.orElseThrow(), tipo);
        }

        UUID alumnoId = alumnoRepository.findStudentIdByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un alumno o tutor asociado al usuario: " + userId));
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un alumno asociado al usuario: " + userId));

        return latestPendingSuggestionForStudent(alumno, tipo)
                .map(List::of)
                .orElseGet(List::of);
    }

    private List<StudentPreferenceResult> executeForParent(Tutor tutor, TipoSugerenciaIA tipo) {
        return tutor.getHijos().stream()
                .flatMap(child -> latestPendingSuggestionForChild(child, tipo).stream())
                .toList();
    }

    private Optional<StudentPreferenceResult> latestPendingSuggestionForChild(
            Alumno child,
            TipoSugerenciaIA tipo
    ) {
        List<SugerenciaIAEntity> suggestions =
                sugerenciaIARepository.findPendingParentNotificationsByAlumnoId(
                        child.getId(), tipo);

        if (suggestions.isEmpty()) {
            return Optional.empty();
        }

        SugerenciaIAEntity latest = suggestions.get(0);
        latest.setAceptada(true);
        latest.setRespondidaEn(LocalDateTime.now());
        sugerenciaIARepository.guardar(latest);

        return Optional.of(new StudentPreferenceResult(latest, child, true));
    }

    private Optional<StudentPreferenceResult> latestPendingSuggestionForStudent(
            Alumno student,
            TipoSugerenciaIA tipo
    ) {
        List<SugerenciaIAEntity> suggestions =
                sugerenciaIARepository.findPendingStudentNotificationsByAlumnoId(
                        student.getId(), tipo);

        if (suggestions.isEmpty()) {
            return Optional.empty();
        }

        SugerenciaIAEntity latest = suggestions.get(0);
        latest.setAceptada(false);
        latest.setRespondidaEn(LocalDateTime.now());
        sugerenciaIARepository.guardar(latest);

        return Optional.of(new StudentPreferenceResult(latest, student, false));
    }

    public record StudentPreferenceResult(
            SugerenciaIAEntity suggestion,
            Alumno child,
            boolean parentView
    ) {
    }
}
