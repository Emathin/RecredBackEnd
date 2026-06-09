package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.RestriccionHoraria;

import com.recred.backend.application.CreateTimeRestriction;
import com.recred.backend.application.DisableTimeRestriction;
import com.recred.backend.application.GetTimeRestrictionsByStudent;
import com.recred.backend.application.TimeRestrictionCommand;
import com.recred.backend.application.UpdateTimeRestriction;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/time-restrictions")
public class TimeRestrictionController {

    private final CreateTimeRestriction createTimeRestriction;
    private final GetTimeRestrictionsByStudent getTimeRestrictionsByStudent;
    private final UpdateTimeRestriction updateTimeRestriction;
    private final DisableTimeRestriction disableTimeRestriction;

    public TimeRestrictionController(
            CreateTimeRestriction createTimeRestriction,
            GetTimeRestrictionsByStudent getTimeRestrictionsByStudent,
            UpdateTimeRestriction updateTimeRestriction,
            DisableTimeRestriction disableTimeRestriction) {
        this.createTimeRestriction = createTimeRestriction;
        this.getTimeRestrictionsByStudent = getTimeRestrictionsByStudent;
        this.updateTimeRestriction = updateTimeRestriction;
        this.disableTimeRestriction = disableTimeRestriction;
    }

    @PostMapping
    public ResponseEntity<RestriccionHoraria> create(
            @RequestBody TimeRestrictionCommand command,
            java.security.Principal principal) {
        RestriccionHoraria created = createTimeRestriction.execute(command, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<RestriccionHoraria>>
    getByStudent(@PathVariable UUID studentId) {
        List<RestriccionHoraria> restrictions =
                getTimeRestrictionsByStudent.execute(studentId);
        return ResponseEntity.ok(restrictions);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestriccionHoraria> update(
            @PathVariable UUID id,
            @RequestBody TimeRestrictionCommand command) {
        RestriccionHoraria updated = updateTimeRestriction.execute(id, command);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<Void> disable(@PathVariable UUID id) {
        disableTimeRestriction.execute(id);
        return ResponseEntity.noContent().build();
    }
}