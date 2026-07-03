package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.SugerenciaIARepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    public GetStudentPreferencesResponse execute(GetStudentPreferencesRequest request) {
        request.validate();

        if (request.isByUser()) {
            return preferencesForUser(request);
        }

        List<SugerenciaIAEntity> preferences = findPreferences(request);
        List<SugerenciaIAEntity> selectedPreferences = selectPreferences(preferences, request);

        return toResponse(selectedPreferences, request.studentId());
    }

    private List<SugerenciaIAEntity> findPreferences(GetStudentPreferencesRequest request) {
        if (request.type() != null) {
            return sugerenciaIARepository.findByAlumnoIdAndTipo(
                    request.studentId(),
                    request.type());
        }

        return sugerenciaIARepository.findByAlumnoId(request.studentId());
    }

    private List<SugerenciaIAEntity> selectPreferences(
            List<SugerenciaIAEntity> preferences,
            GetStudentPreferencesRequest request
    ) {
        if (!request.onlyLast() || preferences.isEmpty()) {
            return preferences;
        }

        return List.of(preferences.get(0));
    }

    @Transactional
    private GetStudentPreferencesResponse preferencesForUser(GetStudentPreferencesRequest request) {
        Optional<Tutor> tutor = findTutorForUser(request.userId());
        if (tutor.isPresent()) {
            return preferencesForParent(tutor.orElseThrow(), request.type());
        }

        return preferencesForStudentUser(request.userId(), request.type());
    }

    private Optional<Tutor> findTutorForUser(UUID userId) {
        return tutorRepository.findByUsuarioId(userId);
    }

    private GetStudentPreferencesResponse preferencesForParent(
            Tutor tutor,
            TipoSugerenciaIA tipo) {
        return new GetStudentPreferencesResponse(preferencesForChildren(tutor, tipo));
    }

    private GetStudentPreferencesResponse preferencesForStudentUser(
            UUID userId,
            TipoSugerenciaIA tipo) {
        Alumno alumno = findStudentForUser(userId);
        return toResponse(latestPendingSuggestionForStudent(alumno, tipo));
    }

    private Alumno findStudentForUser(UUID userId) {
        UUID alumnoId = alumnoRepository.findStudentIdByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un alumno o tutor asociado al usuario: "
                                + userId));
        return alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un alumno asociado al usuario: " + userId));
    }

    private List<GetStudentPreferencesResponse.Preference> preferencesForChildren(
            Tutor tutor,
            TipoSugerenciaIA tipo) {
        return tutor.getHijos().stream()
                .flatMap(child -> latestPendingSuggestionForChild(child, tipo).stream())
                .toList();
    }

    private Optional<GetStudentPreferencesResponse.Preference> latestPendingSuggestionForChild(
            Alumno child,
            TipoSugerenciaIA tipo
    ) {
        return latestPendingParentSuggestion(child.getId(), tipo)
                .map(suggestion -> markAsResponded(suggestion, true))
                .map(suggestion -> toPreference(suggestion, child, true));
    }

    private Optional<GetStudentPreferencesResponse.Preference> latestPendingSuggestionForStudent(
            Alumno student,
            TipoSugerenciaIA tipo
    ) {
        return latestPendingStudentSuggestion(student.getId(), tipo)
                .map(suggestion -> markAsResponded(suggestion, false))
                .map(suggestion -> toPreference(suggestion, student, false));
    }

    private Optional<SugerenciaIAEntity> latestPendingParentSuggestion(
            UUID studentId,
            TipoSugerenciaIA tipo
    ) {
        List<SugerenciaIAEntity> suggestions =
                sugerenciaIARepository.findPendingParentNotificationsByAlumnoId(
                        studentId, tipo);
        return firstSuggestion(suggestions);
    }

    private Optional<SugerenciaIAEntity> latestPendingStudentSuggestion(
            UUID studentId,
            TipoSugerenciaIA tipo
    ) {
        List<SugerenciaIAEntity> suggestions =
                sugerenciaIARepository.findPendingStudentNotificationsByAlumnoId(
                        studentId, tipo);
        return firstSuggestion(suggestions);
    }

    private Optional<SugerenciaIAEntity> firstSuggestion(
            List<SugerenciaIAEntity> suggestions) {
        if (suggestions.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(suggestions.get(0));
    }

    private SugerenciaIAEntity markAsResponded(
            SugerenciaIAEntity suggestion,
            boolean accepted) {
        suggestion.setAceptada(accepted);
        suggestion.setRespondidaEn(LocalDateTime.now());
        sugerenciaIARepository.guardar(suggestion);
        return suggestion;
    }

    private GetStudentPreferencesResponse.Preference toPreference(
            SugerenciaIAEntity suggestion,
            Alumno student,
            boolean parentView
    ) {
        Usuario user = student == null ? null : student.getUsuario();
        return new GetStudentPreferencesResponse.Preference(
                suggestion.getId(),
                student != null ? student.getId() : studentIdFromSuggestion(suggestion),
                user != null ? user.getId() : null,
                user != null ? user.getNombre() : null,
                user != null ? user.getApellido() : null,
                suggestion.getTipo(),
                suggestion.getTitulo(),
                suggestion.getMensaje(),
                metadataValue(suggestion, "productoId"),
                metadataValue(suggestion, "razonIA"),
                parentView
        );
    }

    private GetStudentPreferencesResponse toResponse(
            Optional<GetStudentPreferencesResponse.Preference> preference
    ) {
        return new GetStudentPreferencesResponse(
                preference.map(List::of).orElseGet(List::of));
    }

    private GetStudentPreferencesResponse toResponse(
            List<SugerenciaIAEntity> suggestions,
            UUID studentId
    ) {
        return new GetStudentPreferencesResponse(
                suggestions.stream()
                        .map(suggestion -> toPreference(suggestion, studentId))
                        .toList()
        );
    }

    private GetStudentPreferencesResponse.Preference toPreference(
            SugerenciaIAEntity suggestion,
            UUID studentId
    ) {
        return new GetStudentPreferencesResponse.Preference(
                suggestion.getId(),
                studentIdFromSuggestion(suggestion, studentId),
                null,
                null,
                null,
                suggestion.getTipo(),
                suggestion.getTitulo(),
                suggestion.getMensaje(),
                metadataValue(suggestion, "productoId"),
                metadataValue(suggestion, "razonIA"),
                false
        );
    }

    private UUID studentIdFromSuggestion(SugerenciaIAEntity suggestion, UUID fallbackStudentId) {
        UUID suggestionStudentId = studentIdFromSuggestion(suggestion);
        return suggestionStudentId == null ? fallbackStudentId : suggestionStudentId;
    }

    private UUID studentIdFromSuggestion(SugerenciaIAEntity suggestion) {
        if (suggestion.getAlumnoEntity() == null) {
            return null;
        }
        return suggestion.getAlumnoEntity().getId();
    }

    private String metadataValue(SugerenciaIAEntity suggestion, String key) {
        Map<String, Object> metadata = suggestion.getMetadata();
        if (metadata == null || metadata.get(key) == null) {
            return null;
        }
        return metadata.get(key).toString();
    }
}
