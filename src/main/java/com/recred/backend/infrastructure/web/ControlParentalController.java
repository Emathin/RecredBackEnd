package com.recred.backend.infrastructure.web;

import com.recred.backend.application.AddHealthRestriction;
import com.recred.backend.application.BloquearProductoManual;
import com.recred.backend.application.DesbloquearProductoManual;
import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.application.GetHealthRestrictions;
import com.recred.backend.application.ManageRestrictions;
import com.recred.backend.application.RemoveHealthRestrictions;
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
    private final GetHealthRestrictions obtenerRestriccionesUseCase;
    private final AddHealthRestriction addHealthRestriction;
    private final RemoveHealthRestrictions removeHealthRestrictions;
    private final DetectStudentPreferences detectarPreferenciasUseCase;
    private final BloquearProductoManual bloquearProductoManual;
    private final DesbloquearProductoManual desbloquearProductoManual;

    public ControlParentalController(ManageRestrictions useCase,
                                     GetHealthRestrictions obtenerRestriccionesUseCase,
                                     AddHealthRestriction addHealthRestriction,
                                     RemoveHealthRestrictions removeHealthRestrictions,
                                     DetectStudentPreferences detectarPreferenciasUseCase,
                                     BloquearProductoManual bloquearProductoManual,
                                     DesbloquearProductoManual desbloquearProductoManual) {
        this.useCase = useCase;
        this.obtenerRestriccionesUseCase = obtenerRestriccionesUseCase;
        this.addHealthRestriction = addHealthRestriction;
        this.removeHealthRestrictions = removeHealthRestrictions;
        this.detectarPreferenciasUseCase = detectarPreferenciasUseCase;
        this.bloquearProductoManual = bloquearProductoManual;
        this.desbloquearProductoManual = desbloquearProductoManual;
    }

    @PutMapping("/alumnos/{alumnoId}/actualizar-restricciones-salud")
    public ResponseEntity<Void> actualizarRestriccionesSalud(
            @PathVariable UUID alumnoId,
            @RequestBody ActualizarRestriccionesDTO dto) {

        useCase.execute(alumnoId, dto.getClasificacionesIds());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/alumnos/{alumnoId}/obtener-restricciones-salud")
    public ResponseEntity<List<ClasificacionSaludDTO>> obtenerRestriccionesSalud (
            @PathVariable UUID alumnoId) {

        var restriccionesDominio = obtenerRestriccionesUseCase.ejecutar(alumnoId);
        List<ClasificacionSaludDTO> respuesta = restriccionesDominio.stream()
                .map(ClasificacionSaludDTO::fromDomain)
                .collect(Collectors.toList());

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/alumnos/{alumnoId}/restricciones-salud/{clasificacionId}")
    public ResponseEntity<Void> agregarRestriccionSalud(
            @PathVariable UUID alumnoId,
            @PathVariable UUID clasificacionId) {

        addHealthRestriction.execute(alumnoId, clasificacionId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/alumnos/{alumnoId}/restricciones-salud/{clasificacionId}")
    public ResponseEntity<Void> removerRestriccionSalud(
            @PathVariable UUID alumnoId,
            @PathVariable UUID clasificacionId) {
        removeHealthRestrictions.ejecutar(alumnoId, clasificacionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/alumnos/{alumnoId}/analizar-preferencias")
    public ResponseEntity<Void> analizarPreferencias(
            @PathVariable UUID alumnoId,
            @RequestParam UUID buffetId) {
        detectarPreferenciasUseCase.execute(alumnoId, buffetId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/alumnos/{alumnoId}/productos-bloqueados/{productoId}")
    public ResponseEntity<Void> bloquearProducto(
            @PathVariable UUID alumnoId,
            @PathVariable UUID productoId) {

        bloquearProductoManual.execute(alumnoId, productoId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/alumnos/{alumnoId}/productos-bloqueados/{productoId}")
    public ResponseEntity<Void> desbloquearProducto(
            @PathVariable UUID alumnoId,
            @PathVariable UUID productoId) {

        desbloquearProductoManual.execute(alumnoId, productoId);
        return ResponseEntity.ok().build();
    }
}
