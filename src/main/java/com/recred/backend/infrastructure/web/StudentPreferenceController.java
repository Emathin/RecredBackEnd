package com.recred.backend.infrastructure.web;

import com.recred.backend.application.AgregarProductoFavorito;
import com.recred.backend.application.AgregarProductoFavoritoRequest;
import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.GetFavoritosByAlumno;
import com.recred.backend.application.GetFavoritosByAlumnoRequest;
import com.recred.backend.application.GetStudentPreferences;
import com.recred.backend.application.GetStudentPreferences.StudentPreferenceResult;
import com.recred.backend.application.RemoverProductoFavorito;
import com.recred.backend.application.RemoverProductoFavoritoRequest;
import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.domain.models.TipoSugerenciaIA;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1")
public class StudentPreferenceController {

    private final DetectStudentPreferences detectStudentPreferences;
    private final GetStudentPreferences getStudentPreferences;
    private final RespondToAISuggestion respondToAISuggestion;
    private final AlumnoRepository alumnoRepository;
    private final AgregarProductoFavorito agregarProductoFavorito;
    private final RemoverProductoFavorito removerProductoFavorito;
    private final GetFavoritosByAlumno getFavoritosByAlumno;

    public StudentPreferenceController(
            DetectStudentPreferences detectStudentPreferences,
            GetStudentPreferences getStudentPreferences,
            RespondToAISuggestion respondToAISuggestion,
            AlumnoRepository alumnoRepository,
            AgregarProductoFavorito agregarProductoFavorito,
            RemoverProductoFavorito removerProductoFavorito,
            GetFavoritosByAlumno getFavoritosByAlumno) {
        this.detectStudentPreferences = detectStudentPreferences;
        this.getStudentPreferences = getStudentPreferences;
        this.respondToAISuggestion = respondToAISuggestion;
        this.alumnoRepository = alumnoRepository;
        this.agregarProductoFavorito = agregarProductoFavorito;
        this.removerProductoFavorito = removerProductoFavorito;
        this.getFavoritosByAlumno = getFavoritosByAlumno;
    }

    @PostMapping("/usuarios/{userId}/preferencias/detectar")
    public ResponseEntity<StudentPreferenceResponse> detect(
            @PathVariable UUID userId,
            @RequestParam("buffetId") UUID buffetId
    ) {
        UUID resolvedAlumnoId = resolveAlumnoId(userId);
        return detectStudentPreferences.execute(resolvedAlumnoId, buffetId)
                .map(StudentPreferenceResponse::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/usuarios/{userId}/preferencias")
    public ResponseEntity<List<StudentPreferenceResponse>> getPreferences(
            @PathVariable UUID userId,
            @RequestParam(name = "ultima", required = false, defaultValue = "false") boolean ultima,
            @RequestParam(name = "tipo", required = false) TipoSugerenciaIA tipo
    ) {
        List<StudentPreferenceResponse> preferences =
                getStudentPreferences.executeByUserId(userId, ultima, tipo)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return preferences.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(preferences);
    }

    private StudentPreferenceResponse toResponse(StudentPreferenceResult result) {
        if (result.parentView()) {
            return StudentPreferenceResponse.fromParentView(
                    result.suggestion(),
                    result.child());
        }
        return StudentPreferenceResponse.fromStudentView(
                result.suggestion(),
                result.child());
    }

    @PatchMapping("/usuarios/{userId}/preferencias/{sugerenciaId}/responder")
    public ResponseEntity<Void> respond(
            @PathVariable UUID sugerenciaId,
            @RequestParam("aceptada") boolean aceptada
    ) {
        respondToAISuggestion.execute(sugerenciaId, aceptada);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/alumnos/{alumnoId}/preferencias/favoritos/{productoId}")
    public ResponseEntity<Void> agregarFavorito(
            @PathVariable UUID alumnoId,
            @PathVariable UUID productoId
    ) {
        agregarProductoFavorito.execute(new AgregarProductoFavoritoRequest(alumnoId, productoId));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/alumnos/{alumnoId}/preferencias/favoritos/{productoId}")
    public ResponseEntity<Void> removerFavorito(
            @PathVariable UUID alumnoId,
            @PathVariable UUID productoId
    ) {
        removerProductoFavorito.execute(new RemoverProductoFavoritoRequest(alumnoId, productoId));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/alumnos/{alumnoId}/preferencias/favoritos")
    public ResponseEntity<List<ProductResponseDTO>> obtenerFavoritos(
            @PathVariable UUID alumnoId
    ) {
        List<ProductResponseDTO> favoritos =
                getFavoritosByAlumno.execute(new GetFavoritosByAlumnoRequest(alumnoId))
                        .stream()
                        .map(ProductResponseDTO::fromUseCaseResponse)
                        .toList();
        return ResponseEntity.ok(favoritos);
    }

    private UUID resolveAlumnoId(UUID userId) {
        return alumnoRepository.findStudentIdByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un alumno asociado al usuario: " + userId));
    }
}
