package com.recred.backend.infrastructure.web;

import com.recred.backend.application.PredecirGastoAlumno;
import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.infrastructure.web.PrediccionGastoResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ia/alumnos/{alumnoId}/prediccion-gasto")
public class PrediccionGastoController {

    private final PredecirGastoAlumno predecirGastoAlumno;

    public PrediccionGastoController(PredecirGastoAlumno predecirGastoAlumno) {
        this.predecirGastoAlumno = predecirGastoAlumno;
    }

    @GetMapping
    public PrediccionGastoResponse predecir(
            @PathVariable UUID alumnoId,
            @RequestParam(required = false) PeriodoPresupuesto periodo
    ) {
        return PrediccionGastoResponse.fromDomain(predecirGastoAlumno.ejecutar(
                alumnoId,
                periodo
        ));
    }
}
