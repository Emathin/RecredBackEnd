package com.recred.backend.control_parental.infrastructure.web;

import com.recred.backend.control_parental.application.usecases.ActualizarRestriccionesSaludUseCase;
import com.recred.backend.control_parental.infrastructure.web.dtos.ActualizarRestriccionesDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/control-parental")
public class ControlParentalController {

    private final ActualizarRestriccionesSaludUseCase useCase;

    // Inyección de dependencia por constructor
    public ControlParentalController(ActualizarRestriccionesSaludUseCase useCase) {
        this.useCase = useCase;
    }

    @PutMapping("/alumnos/{alumnoId}/restricciones-salud")
    public ResponseEntity<Void> actualizarRestriccionesSalud(
            @PathVariable UUID alumnoId,
            @RequestBody ActualizarRestriccionesDTO dto) {

        useCase.ejecutar(alumnoId, dto.getClasificacionesIds());
        return ResponseEntity.ok().build();
    }
}