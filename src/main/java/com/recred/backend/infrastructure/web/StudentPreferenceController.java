package com.recred.backend.infrastructure.web;

import com.recred.backend.application.AddFavoriteProduct;
import com.recred.backend.application.AddFavoriteProductRequest;
import com.recred.backend.application.DeleteFavoriteProduct;
import com.recred.backend.application.DeleteFavoriteProductRequest;
import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.DetectStudentPreferencesRequest;
import com.recred.backend.application.GetFavoritesByStudent;
import com.recred.backend.application.GetFavoritesByStudentRequest;
import com.recred.backend.application.GetStudentPreferences;
import com.recred.backend.application.GetStudentPreferencesByUserRequest;
import com.recred.backend.application.GetStudentPreferencesRequest;
import com.recred.backend.application.RespondToAISuggestion;
import com.recred.backend.application.RespondToAISuggestionRequest;
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
    private final AddFavoriteProduct addFavoriteProduct;
    private final DeleteFavoriteProduct deleteFavoriteProduct;
    private final GetFavoritesByStudent getFavoritesByStudent;

    public StudentPreferenceController(
            DetectStudentPreferences detectStudentPreferences,
            GetStudentPreferences getStudentPreferences,
            RespondToAISuggestion respondToAISuggestion,
            AlumnoRepository alumnoRepository,
            AddFavoriteProduct addFavoriteProduct,
            DeleteFavoriteProduct deleteFavoriteProduct,
            GetFavoritesByStudent getFavoritesByStudent) {
        this.detectStudentPreferences = detectStudentPreferences;
        this.getStudentPreferences = getStudentPreferences;
        this.respondToAISuggestion = respondToAISuggestion;
        this.alumnoRepository = alumnoRepository;
        this.addFavoriteProduct = addFavoriteProduct;
        this.deleteFavoriteProduct = deleteFavoriteProduct;
        this.getFavoritesByStudent = getFavoritesByStudent;
    }

    @PostMapping("/usuarios/{userId}/preferencias/detectar")
    public ResponseEntity<StudentPreferenceResponseDTO> detect(
            @PathVariable UUID userId,
            @RequestParam("buffetId") UUID buffetId
    ) {
        UUID resolvedAlumnoId = resolveAlumnoId(userId);
        return detectStudentPreferences
                .execute(new DetectStudentPreferencesRequest(resolvedAlumnoId, buffetId))
                .map(StudentPreferenceResponseDTO::fromUseCaseResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/usuarios/{userId}/preferencias")
    public ResponseEntity<List<StudentPreferenceResponseDTO>> getPreferences(
            @PathVariable UUID userId,
            @RequestParam(name = "ultima", required = false, defaultValue = "false") boolean ultima,
            @RequestParam(name = "tipo", required = false) TipoSugerenciaIA tipo
    ) {
        List<StudentPreferenceResponseDTO> preferences =
                getStudentPreferences.execute(
                        GetStudentPreferencesRequest.byUser(userId, ultima, tipo))
                        .preferences()
                        .stream()
                        .map(StudentPreferenceResponseDTO::fromApplication)
                        .toList();

        return preferences.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(preferences);
    }

    @GetMapping("/alumnos/{alumnoId}/preferencias")
    public ResponseEntity<List<StudentPreferenceResponseDTO>> getPreferencesByAlumno(
            @PathVariable UUID alumnoId,
            @RequestParam(name = "ultima", required = false, defaultValue = "false") boolean ultima,
            @RequestParam(name = "tipo", required = false) TipoSugerenciaIA tipo
    ) {
        List<StudentPreferenceResponseDTO> preferences =
                getStudentPreferences.execute(
                        new GetStudentPreferencesRequest(alumnoId, ultima, tipo))
                        .preferences()
                        .stream()
                        .map(StudentPreferenceResponseDTO::fromApplication)
                        .toList();

        return preferences.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(preferences);
    }

    @PatchMapping("/usuarios/{userId}/preferencias/{sugerenciaId}/responder")
    public ResponseEntity<Void> respond(
            @PathVariable UUID sugerenciaId,
            @RequestParam("aceptada") boolean aceptada
    ) {
        respondToAISuggestion.execute(new RespondToAISuggestionRequest(sugerenciaId, aceptada));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/alumnos/{alumnoId}/preferencias/favoritos/{productoId}")
    public ResponseEntity<Void> agregarFavorito(
            @PathVariable UUID alumnoId,
            @PathVariable UUID productoId
    ) {
        addFavoriteProduct.execute(new AddFavoriteProductRequest(alumnoId, productoId));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/alumnos/{alumnoId}/preferencias/favoritos/{productoId}")
    public ResponseEntity<Void> removerFavorito(
            @PathVariable UUID alumnoId,
            @PathVariable UUID productoId
    ) {
        deleteFavoriteProduct.execute(new DeleteFavoriteProductRequest(alumnoId, productoId));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/alumnos/{alumnoId}/preferencias/favoritos")
    public ResponseEntity<List<ProductResponseDTO>> obtenerFavoritos(
            @PathVariable UUID alumnoId
    ) {
        List<ProductResponseDTO> favoritos =
                getFavoritesByStudent.execute(new GetFavoritesByStudentRequest(alumnoId))
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
