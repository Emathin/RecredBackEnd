package com.recred.backend.infrastructure.web;

import com.recred.backend.application.AddHealthRestriction;
import com.recred.backend.application.AddHealthRestrictionRequest;
import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.DetectStudentPreferencesRequest;
import com.recred.backend.application.GetHealthRestrictions;
import com.recred.backend.application.GetHealthRestrictionsRequest;
import com.recred.backend.application.ManageRestrictions;
import com.recred.backend.application.ManageRestrictionsRequest;
import com.recred.backend.application.ManualProductLock;
import com.recred.backend.application.ManualProductLockRequest;
import com.recred.backend.application.ManualProductUnlock;
import com.recred.backend.application.ManualProductUnlockRequest;
import com.recred.backend.application.RemoveHealthRestrictions;
import com.recred.backend.application.RemoveHealthRestrictionsRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/control-parental")
public class ControlParentalController {

    private final ManageRestrictions useCase;
    private final GetHealthRestrictions obtenerRestriccionesUseCase;
    private final AddHealthRestriction addHealthRestriction;
    private final RemoveHealthRestrictions removeHealthRestrictions;
    private final DetectStudentPreferences detectarPreferenciasUseCase;
    private final ManualProductLock manualProductLock;
    private final ManualProductUnlock manualProductUnlock;
    private final TutorAccessGuard tutorAccessGuard;
    private final PlanAccessGuard planAccessGuard;

    public ControlParentalController(ManageRestrictions useCase,
                                     GetHealthRestrictions obtenerRestriccionesUseCase,
                                     AddHealthRestriction addHealthRestriction,
                                     RemoveHealthRestrictions removeHealthRestrictions,
                                     DetectStudentPreferences detectarPreferenciasUseCase,
                                     ManualProductLock manualProductLock,
                                     ManualProductUnlock manualProductUnlock,
                                     TutorAccessGuard tutorAccessGuard,
                                     PlanAccessGuard planAccessGuard) {
        this.useCase = useCase;
        this.obtenerRestriccionesUseCase = obtenerRestriccionesUseCase;
        this.addHealthRestriction = addHealthRestriction;
        this.removeHealthRestrictions = removeHealthRestrictions;
        this.detectarPreferenciasUseCase = detectarPreferenciasUseCase;
        this.manualProductLock = manualProductLock;
        this.manualProductUnlock = manualProductUnlock;
        this.tutorAccessGuard = tutorAccessGuard;
        this.planAccessGuard = planAccessGuard;
    }

    @PutMapping("/alumnos/{alumnoId}/actualizar-restricciones-salud")
    public ResponseEntity<Void> actualizarRestriccionesSalud(
            @PathVariable UUID alumnoId,
            @RequestBody ActualizarRestriccionesDTO dto,
            Authentication authentication) {
        tutorAccessGuard.requireStrictTutorStudentAccess(authentication, alumnoId);
        useCase.execute(new ManageRestrictionsRequest(alumnoId, dto.getClasificacionesIds()));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/alumnos/{alumnoId}/obtener-restricciones-salud")
    public ResponseEntity<List<ClasificacionSaludDTO>> obtenerRestriccionesSalud (
            @PathVariable UUID alumnoId,
            Authentication authentication) {
        tutorAccessGuard.requireStudentAccess(authentication, alumnoId);
        var restriccionesResponse = obtenerRestriccionesUseCase.execute(
                new GetHealthRestrictionsRequest(alumnoId)
        );
        List<ClasificacionSaludDTO> respuesta = restriccionesResponse.stream()
                .map(res -> new ClasificacionSaludDTO(res.id(), res.descripcion()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/alumnos/{alumnoId}/restricciones-salud/{clasificacionId}")
    public ResponseEntity<Void> agregarRestriccionSalud(
            @PathVariable UUID alumnoId,
            @PathVariable UUID clasificacionId,
            Authentication authentication) {
        tutorAccessGuard.requireStrictTutorStudentAccess(authentication, alumnoId);
        addHealthRestriction.execute(new AddHealthRestrictionRequest(alumnoId, clasificacionId));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/alumnos/{alumnoId}/restricciones-salud/{clasificacionId}")
    public ResponseEntity<Void> removerRestriccionSalud(
            @PathVariable UUID alumnoId,
            @PathVariable UUID clasificacionId,
            Authentication authentication) {
        tutorAccessGuard.requireStrictTutorStudentAccess(authentication, alumnoId);
        removeHealthRestrictions.execute(
                new RemoveHealthRestrictionsRequest(alumnoId, clasificacionId)
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/alumnos/{alumnoId}/analizar-preferencias")
    public ResponseEntity<Void> analizarPreferencias(
            @PathVariable UUID alumnoId,
            @RequestParam UUID buffetId,
            Authentication authentication) {
        tutorAccessGuard.requireStrictTutorStudentAccess(authentication, alumnoId);
        planAccessGuard.requireTutorIntermediate(authentication);
        detectarPreferenciasUseCase.execute(
                new DetectStudentPreferencesRequest(alumnoId, buffetId)
        );
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/alumnos/{alumnoId}/productos-bloqueados/{productoId}")
    public ResponseEntity<Void> bloquearProducto(
            @PathVariable UUID alumnoId,
            @PathVariable UUID productoId,
            Authentication authentication) {
        tutorAccessGuard.requireStrictTutorStudentAccess(authentication, alumnoId);
        manualProductLock.execute(new ManualProductLockRequest(alumnoId, productoId));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/alumnos/{alumnoId}/productos-bloqueados/{productoId}")
    public ResponseEntity<Void> desbloquearProducto(
            @PathVariable UUID alumnoId,
            @PathVariable UUID productoId,
            Authentication authentication) {
        tutorAccessGuard.requireStrictTutorStudentAccess(authentication, alumnoId);
        manualProductUnlock.execute(
                new ManualProductUnlockRequest(alumnoId, productoId)
        );
        return ResponseEntity.ok().build();
    }
}
