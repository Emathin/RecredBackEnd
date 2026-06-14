package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetTimeSlotsBySchool;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({
        "/api/colegios/{colegioId}/franjas-horarias",
        "/api/v1/colegios/{colegioId}/franjas-horarias"
})
public class FranjaHorariaController {

    private final GetTimeSlotsBySchool getTimeSlotsBySchool;

    public FranjaHorariaController(GetTimeSlotsBySchool getTimeSlotsBySchool) {
        this.getTimeSlotsBySchool = getTimeSlotsBySchool;
    }

    @GetMapping
    public ResponseEntity<List<TimeSlotResponse>> obtenerFranjas(@PathVariable UUID colegioId) {
        List<TimeSlotResponse> franjas = getTimeSlotsBySchool.execute(colegioId)
                .stream()
                .map(TimeSlotResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(franjas);
    }
}
