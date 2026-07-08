package com.recred.backend.infrastructure.web;

import com.recred.backend.application.CreateTimeRestrictionResponse;
import com.recred.backend.application.CreateTimeRestrictionRequest;
import com.recred.backend.application.CreateTimeRestriction;
import com.recred.backend.application.DisableTimeRestriction;
import com.recred.backend.application.DisableTimeRestrictionRequest;
import com.recred.backend.application.GetTimeRestrictionsByStudent;
import com.recred.backend.application.GetTimeRestrictionsByStudentRequest;
import com.recred.backend.application.GetTimeRestrictionsByStudentResponse;
import com.recred.backend.application.UpdateTimeRestriction;
import com.recred.backend.application.UpdateTimeRestrictionRequest;
import com.recred.backend.application.UpdateTimeRestrictionResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/v1/time-restrictions")
public class TimeRestrictionController {

    private final CreateTimeRestriction createTimeRestriction;
    private final GetTimeRestrictionsByStudent getTimeRestrictionsByStudent;
    private final UpdateTimeRestriction updateTimeRestriction;
    private final DisableTimeRestriction disableTimeRestriction;
    private final TutorAccessGuard tutorAccessGuard;

    public TimeRestrictionController(
            CreateTimeRestriction createTimeRestriction,
            GetTimeRestrictionsByStudent getTimeRestrictionsByStudent,
            UpdateTimeRestriction updateTimeRestriction,
            DisableTimeRestriction disableTimeRestriction,
            TutorAccessGuard tutorAccessGuard) {
        this.createTimeRestriction = createTimeRestriction;
        this.getTimeRestrictionsByStudent = getTimeRestrictionsByStudent;
        this.updateTimeRestriction = updateTimeRestriction;
        this.disableTimeRestriction = disableTimeRestriction;
        this.tutorAccessGuard = tutorAccessGuard;
    }

    @PostMapping
    public ResponseEntity<CreateTimeRestrictionResponse> create(
            @RequestBody CreateTimeRestrictionRequest command,
            java.security.Principal principal,
            Authentication authentication) {
        tutorAccessGuard.requireStrictTutorStudentAccess(authentication, command.studentId());
        CreateTimeRestrictionRequest request = new CreateTimeRestrictionRequest(
                command.studentId(),
                command.timeSlotId(),
                command.categoryId(),
                command.classificationId(),
                principal.getName()
        );
        CreateTimeRestrictionResponse created = createTimeRestriction.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<GetTimeRestrictionsByStudentResponse>>
    getByStudent(@PathVariable UUID studentId, Authentication authentication) {
        tutorAccessGuard.requireStudentAccess(authentication, studentId);
        List<GetTimeRestrictionsByStudentResponse> restrictions =
                getTimeRestrictionsByStudent.execute(
                        new GetTimeRestrictionsByStudentRequest(studentId));
        return ResponseEntity.ok(restrictions);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UpdateTimeRestrictionResponse> update(
            @PathVariable UUID id,
            @RequestBody UpdateTimeRestrictionRequest requestBody,
            Authentication authentication) {
        tutorAccessGuard.requireTimeRestrictionAccess(authentication, id);
        UpdateTimeRestrictionRequest request = new UpdateTimeRestrictionRequest(
                id,
                requestBody.timeSlotId(),
                requestBody.categoryId(),
                requestBody.classificationId()
        );
        UpdateTimeRestrictionResponse updated = updateTimeRestriction.execute(request);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<Void> disable(@PathVariable UUID id, Authentication authentication) {
        tutorAccessGuard.requireTimeRestrictionAccess(authentication, id);
        disableTimeRestriction.execute(new DisableTimeRestrictionRequest(id));
        return ResponseEntity.noContent().build();
    }
}
