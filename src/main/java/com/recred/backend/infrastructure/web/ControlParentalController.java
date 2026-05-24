package com.recred.backend.infrastructure.web;

import com.recred.backend.application.AdministrarRestricciones;
import com.recred.backend.application.AgregarRestriccionSalud;
import com.recred.backend.application.ObtenerRestriccionesSalud;
import com.recred.backend.application.RemoverRestriccionSalud;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/control-parental")
public class ControlParentalController {

    private final AdministrarRestricciones useCase;
    private final ObtenerRestriccionesSalud obtenerRestriccionesUseCase;
    private final AgregarRestriccionSalud agregarRestriccionSalud;
    private final RemoverRestriccionSalud removerRestriccionSalud;

    public ControlParentalController(AdministrarRestricciones useCase,
                                     ObtenerRestriccionesSalud obtenerRestriccionesUseCase,
                                     AgregarRestriccionSalud agregarRestriccionSalud,
            RemoverRestriccionSalud removerRestriccionSalud) {
        this.useCase = useCase;
        this.obtenerRestriccionesUseCase = obtenerRestriccionesUseCase;
        this.agregarRestriccionSalud = agregarRestriccionSalud;
        this.removerRestriccionSalud = removerRestriccionSalud;
    }

    @PutMapping("/alumnos/{alumnoId}/actualizar-restricciones-salud")
    public ResponseEntity<Void> actualizarRestriccionesSalud(
            @PathVariable UUID alumnoId,
            @RequestBody ActualizarRestriccionesDTO dto) {

        useCase.ejecutar(alumnoId, dto.getClasificacionesIds());
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

        agregarRestriccionSalud.ejecutar(alumnoId, clasificacionId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/alumnos/{alumnoId}/restricciones-salud/{clasificacionId}")
    public ResponseEntity<Void> removerRestriccionSalud(
            @PathVariable UUID alumnoId,
            @PathVariable UUID clasificacionId) {
        removerRestriccionSalud.ejecutar(alumnoId, clasificacionId);
        return ResponseEntity.noContent().build();
    }

}