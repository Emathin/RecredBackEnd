package com.recred.backend.infrastructure.web;

import com.recred.backend.application.ObtenerHistorialAlumno;
import com.recred.backend.application.ObtenerHistorialTutor;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/purchases")
public class PurchaseHistoryController {

    private final ObtenerHistorialAlumno obtenerHistorialAlumno;
    private final ObtenerHistorialTutor obtenerHistorialTutor;

    public PurchaseHistoryController(ObtenerHistorialAlumno obtenerHistorialAlumno,
                                     ObtenerHistorialTutor obtenerHistorialTutor) {
        this.obtenerHistorialAlumno = obtenerHistorialAlumno;
        this.obtenerHistorialTutor = obtenerHistorialTutor;
    }

    @GetMapping("/alumno/{alumnoId}")
    public ResponseEntity<List<PurchaseResponseDTO>> getHistorialAlumno
            (@PathVariable UUID alumnoId) {
        List<PurchaseResponseDTO> historial = obtenerHistorialAlumno.execute(alumnoId)
                .stream()
                .map(PurchaseResponseDTO::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(historial);
    }

    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<List<PurchaseResponseDTO>> getHistorialTutor(@PathVariable UUID tutorId) {
        List<PurchaseResponseDTO> historial = obtenerHistorialTutor.execute(tutorId)
                .stream()
                .map(PurchaseResponseDTO::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(historial);
    }
}