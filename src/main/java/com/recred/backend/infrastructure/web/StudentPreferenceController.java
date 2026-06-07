package com.recred.backend.infrastructure.web;

import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.GetStudentPreferences;
import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({
        "/api/v1/alumnos/{alumnoId}/preferencias",
        "/api/v1/usuarios/{userId}/preferencias"
})
public class StudentPreferenceController {

    private final DetectStudentPreferences detectStudentPreferences;
    private final GetStudentPreferences getStudentPreferences;
    private final RespondToAISuggestion respondToAISuggestion;
    private final AlumnoRepository alumnoRepository;

    public StudentPreferenceController(
            DetectStudentPreferences detectStudentPreferences,
            GetStudentPreferences getStudentPreferences,
            RespondToAISuggestion respondToAISuggestion,
            AlumnoRepository alumnoRepository) {
        this.detectStudentPreferences = detectStudentPreferences;
        this.getStudentPreferences = getStudentPreferences;
        this.respondToAISuggestion = respondToAISuggestion;
        this.alumnoRepository = alumnoRepository;
    }

    @PostMapping("/detectar")
    public ResponseEntity<StudentPreferenceResponse> detect(
            @PathVariable(required = false) UUID alumnoId,
            @PathVariable(required = false) UUID userId,
            @RequestParam("buffetId") UUID buffetId
    ) {
        UUID resolvedAlumnoId = resolveAlumnoId(alumnoId, userId);
        return detectStudentPreferences.execute(resolvedAlumnoId, buffetId)
                .map(StudentPreferenceResponse::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping
    public ResponseEntity<List<StudentPreferenceResponse>> getPreferences(
            @PathVariable(required = false) UUID alumnoId,
            @PathVariable(required = false) UUID userId,
            @RequestParam(name = "ultima", required = false, defaultValue = "false") boolean ultima,
            @RequestParam(name = "tipo", required = false) TipoSugerenciaIA tipo
    ) {
        UUID resolvedAlumnoId = resolveAlumnoId(alumnoId, userId);
        List<StudentPreferenceResponse> preferences = getStudentPreferences
                .execute(resolvedAlumnoId, ultima, tipo)
                .stream()
                .map(StudentPreferenceResponse::fromEntity)
                .toList();

        return preferences.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(preferences);
    }

    @PatchMapping("/{sugerenciaId}/responder")
    public ResponseEntity<Void> respond(
            @PathVariable UUID sugerenciaId,
            @RequestParam("aceptada") boolean aceptada
    ) {
        respondToAISuggestion.execute(sugerenciaId, aceptada);
        return ResponseEntity.ok().build();
    }

    private UUID resolveAlumnoId(UUID alumnoId, UUID userId) {
        if (alumnoId != null) {
            return alumnoId;
        }
        if (userId != null) {
            return alumnoRepository.findStudentIdByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No existe un alumno asociado al usuario: " + userId));
        }
        throw new IllegalArgumentException("Debe indicar alumnoId o userId");
    }
}
