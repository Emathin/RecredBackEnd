package com.recred.backend.infrastructure.web;

import com.recred.backend.application.ObtenerHijosPorPadre;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tutores")
public class TutorController {

    private final ObtenerHijosPorPadre obtenerHijosPorPadre;

    public TutorController(ObtenerHijosPorPadre obtenerHijosPorPadre) {
        this.obtenerHijosPorPadre = obtenerHijosPorPadre;
    }

    @GetMapping("/{tutorId}/hijos")
    public ResponseEntity<List<StudentDTO>> obtenerHijos(@PathVariable UUID tutorId) {
        List<StudentDTO> alumnos = obtenerHijosPorPadre.execute(tutorId)
                .stream()
                .map(StudentDTO::fromDomain)
                .toList();

        return ResponseEntity.ok(alumnos);
    }
}