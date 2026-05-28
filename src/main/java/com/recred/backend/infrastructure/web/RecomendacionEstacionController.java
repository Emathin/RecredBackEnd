package com.recred.backend.infrastructure.web.controllers;

import com.recred.backend.application.GenerarSugerenciaPorEstacion;
import com.recred.backend.infrastructure.web.RecomendacionResponseDTO;
import com.recred.backend.infrastructure.web.SugerenciaDTO;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recomendaciones")
@Validated
public class RecomendacionEstacionController {
    private final GenerarSugerenciaPorEstacion generarSugerenciaPorEstacion;

    public RecomendacionEstacionController(
            GenerarSugerenciaPorEstacion generarSugerenciaPorEstacion) {
        this.generarSugerenciaPorEstacion = generarSugerenciaPorEstacion;
    }

    @GetMapping("/kiosco")
    public ResponseEntity<RecomendacionResponseDTO> obtenerSugerencias(
    /*
     * @RequestParam String nivelEscolar,
     * 
     * @RequestParam double latitud,
     * 
     * @RequestParam double longitud
     */

    ) {
        RecomendacionResponseDTO respuesta = generarSugerenciaPorEstacion
                .ejecutarLogicaSugerencias();

        System.out.println("****************************************");

        for (SugerenciaDTO sugerencia : respuesta.getSugerencias()) {
            System.out.println("SUGERENCIA: " +
                    sugerencia.getCategoria() + " - " +
                    sugerencia.getAccion() + " - "
                    + sugerencia.getMotivo());
        }

        System.out.println("PROMOCION: " + respuesta.getTipPromocional());
        System.out.println("****************************************");

        return ResponseEntity.ok(respuesta);
    }
}
