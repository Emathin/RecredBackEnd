package com.recred.backend.infrastructure.web;

import com.recred.backend.application.ApproveSchoolRegistration;
import com.recred.backend.application.ApproveSchoolRegistrationRequest;
import com.recred.backend.application.RejectSchoolRegistration;
import com.recred.backend.application.RejectSchoolRegistrationRequest;
import com.recred.backend.application.SubmitSchoolRegistration;
import com.recred.backend.application.SubmitSchoolRegistrationRequest;
import com.recred.backend.application.GetSchoolRegistrations;
import com.recred.backend.application.GetSchoolRegistrationsResponse;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school-registrations")
@RequiredArgsConstructor
public class SchoolRegistrationController {

    private final SubmitSchoolRegistration submitUseCase;
    private final ApproveSchoolRegistration approveUseCase;
    private final RejectSchoolRegistration rejectUseCase;
    private final GetSchoolRegistrations getSchoolRegistrationsUseCase;
    private final AdminAccessGuard adminAccessGuard;

    @PostMapping
    public ResponseEntity<Void> submitRegistration(
            @Valid @RequestBody SubmitSchoolRegistrationRequest request) {
        submitUseCase.execute(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveRegistration(Authentication authentication, 
                                                    @PathVariable UUID id) {
        adminAccessGuard.requireAdminAccess(authentication);
        approveUseCase.execute(new ApproveSchoolRegistrationRequest(id));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectRegistration(Authentication authentication, 
                                                   @PathVariable UUID id) {
        adminAccessGuard.requireAdminAccess(authentication);
        rejectUseCase.execute(new RejectSchoolRegistrationRequest(id));
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<GetSchoolRegistrationsResponse>> getRegistrations(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "PENDING") String status) {
        adminAccessGuard.requireAdminAccess(authentication);
        List<GetSchoolRegistrationsResponse> list = getSchoolRegistrationsUseCase.execute(status);
        return ResponseEntity.ok(list);
    }
}
