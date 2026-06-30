package com.recred.backend.infrastructure.web;

import com.recred.backend.application.ApproveSchoolRegistration;
import com.recred.backend.application.ApproveSchoolRegistrationRequest;
import com.recred.backend.application.RejectSchoolRegistration;
import com.recred.backend.application.RejectSchoolRegistrationRequest;
import com.recred.backend.application.SubmitSchoolRegistration;
import com.recred.backend.application.SubmitSchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationStatus;
import com.recred.backend.domain.repositories.SchoolRegistrationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    private final SchoolRegistrationRequestRepository repository;

    @PostMapping
    public ResponseEntity<Void> submitRegistration(
            @RequestBody SubmitSchoolRegistrationRequest request) {
        submitUseCase.execute(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveRegistration(@PathVariable UUID id) {
        approveUseCase.execute(new ApproveSchoolRegistrationRequest(id));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectRegistration(@PathVariable UUID id) {
        rejectUseCase.execute(new RejectSchoolRegistrationRequest(id));
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<SchoolRegistrationResponseDTO>> getRegistrations(
            @RequestParam(required = false, defaultValue = "PENDING") String status) {
        SchoolRegistrationStatus enumStatus = SchoolRegistrationStatus.valueOf(status);
        List<SchoolRegistrationResponseDTO> list = repository.findByStatus(enumStatus)
                .stream()
                .map(SchoolRegistrationResponseDTO::fromDomain)
                .toList();
        return ResponseEntity.ok(list);
    }
}
