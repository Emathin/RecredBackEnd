package com.recred.backend.infrastructure.web;

import com.recred.backend.application.AddHealthRestriction;
import com.recred.backend.application.BlockProductManually;
import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.GetHealthRestrictions;
import com.recred.backend.application.ManageRestrictions;
import com.recred.backend.application.RemoveHealthRestrictions;
import com.recred.backend.application.UnblockProductManually;
import org.springframework.http.ResponseEntity;
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
    private final GetHealthRestrictions getHealthRestrictions;
    private final AddHealthRestriction addHealthRestriction;
    private final RemoveHealthRestrictions removeHealthRestrictions;
    private final DetectStudentPreferences detectStudentPreferences;
    private final BlockProductManually blockProductManually;
    private final UnblockProductManually unblockProductManually;

    public ControlParentalController(ManageRestrictions useCase,
                                     GetHealthRestrictions getHealthRestrictions,
                                     AddHealthRestriction addHealthRestriction,
                                     RemoveHealthRestrictions removeHealthRestrictions,
                                     DetectStudentPreferences detectStudentPreferences,
                                     BlockProductManually blockProductManually,
                                     UnblockProductManually unblockProductManually) {
        this.useCase = useCase;
        this.getHealthRestrictions = getHealthRestrictions;
        this.addHealthRestriction = addHealthRestriction;
        this.removeHealthRestrictions = removeHealthRestrictions;
        this.detectStudentPreferences = detectStudentPreferences;
        this.blockProductManually = blockProductManually;
        this.unblockProductManually = unblockProductManually;
    }

    @PutMapping("/alumnos/{alumnoId}/actualizar-restricciones-salud")
    public ResponseEntity<Void> updateHealthRestrictions(
            @PathVariable UUID alumnoId,
            @RequestBody ActualizarRestriccionesDTO dto) {

        useCase.execute(alumnoId, dto.getClasificacionesIds());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/alumnos/{alumnoId}/obtener-restricciones-salud")
    public ResponseEntity<List<ClasificacionSaludDTO>> getHealthRestrictions(
            @PathVariable UUID alumnoId) {

        var domainRestrictions = getHealthRestrictions.execute(alumnoId);
        List<ClasificacionSaludDTO> response = domainRestrictions.stream()
                .map(ClasificacionSaludDTO::fromDomain)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/alumnos/{alumnoId}/restricciones-salud/{clasificacionId}")
    public ResponseEntity<Void> addHealthRestriction(
            @PathVariable UUID alumnoId,
            @PathVariable UUID clasificacionId) {

        addHealthRestriction.execute(alumnoId, clasificacionId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/alumnos/{alumnoId}/restricciones-salud/{clasificacionId}")
    public ResponseEntity<Void> removeHealthRestriction(
            @PathVariable UUID alumnoId,
            @PathVariable UUID clasificacionId) {
        removeHealthRestrictions.execute(alumnoId, clasificacionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/alumnos/{alumnoId}/analizar-preferencias")
    public ResponseEntity<Void> analyzePreferences(
            @PathVariable UUID alumnoId,
            @RequestParam UUID buffetId) {
        detectStudentPreferences.execute(alumnoId, buffetId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/alumnos/{alumnoId}/productos-bloqueados/{productoId}")
    public ResponseEntity<Void> blockProduct(
            @PathVariable UUID alumnoId,
            @PathVariable UUID productoId) {

        blockProductManually.execute(alumnoId, productoId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/alumnos/{alumnoId}/productos-bloqueados/{productoId}")
    public ResponseEntity<Void> unblockProduct(
            @PathVariable UUID alumnoId,
            @PathVariable UUID productoId) {

        unblockProductManually.execute(alumnoId, productoId);
        return ResponseEntity.ok().build();
    }
}
