package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetTimeSlotsBySchool;
import com.recred.backend.domain.models.FranjaHoraria;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/colegios/{colegioId}/franjas-horarias")
public class FranjaHorariaController {

    private final GetTimeSlotsBySchool getTimeSlotsBySchool;

    public FranjaHorariaController(GetTimeSlotsBySchool getTimeSlotsBySchool) {
        this.getTimeSlotsBySchool = getTimeSlotsBySchool;
    }

    @GetMapping
    public ResponseEntity<List<FranjaHoraria>> obtenerFranjas(@PathVariable UUID colegioId) {
        List<FranjaHoraria> franjas = getTimeSlotsBySchool.execute(colegioId);
        return ResponseEntity.ok(franjas);
    }
}