package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetTimeSlotsBySchool;
import com.recred.backend.domain.models.TimeBadge;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/colegios/{colegioId}/franjas-horarias")
public class TimeBadgetController {

    private final GetTimeSlotsBySchool getTimeSlotsBySchool;

    public TimeBadgetController(GetTimeSlotsBySchool getTimeSlotsBySchool) {
        this.getTimeSlotsBySchool = getTimeSlotsBySchool;
    }

    @GetMapping
    public ResponseEntity<List<TimeBadge>> getBadges(@PathVariable UUID colegioId) {
        List<TimeBadge> franjas = getTimeSlotsBySchool.execute(colegioId);
        return ResponseEntity.ok(franjas);
    }
}