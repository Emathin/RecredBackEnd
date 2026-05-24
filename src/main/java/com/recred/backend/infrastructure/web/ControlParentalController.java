package com.recred.backend.infrastructure.web;

import com.recred.backend.application.AdministrarRestricciones;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/control-parental")
public class ControlParentalController {

    private final AdministrarRestricciones useCase;

    public ControlParentalController(AdministrarRestricciones useCase) {
        this.useCase = useCase;
    }

    @PutMapping("/alumnos/{alumnoId}/restricciones-salud")
    public ResponseEntity<Void> actualizarRestriccionesSalud(
            @PathVariable UUID alumnoId,
            @RequestBody com.recred.backend.presentacion.ActualizarRestriccionesDTO dto) {

        useCase.ejecutar(alumnoId, dto.getClasificacionesIds());
        return ResponseEntity.ok().build();
    }
}