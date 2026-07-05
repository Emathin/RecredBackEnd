package com.recred.backend.infrastructure.web;

import com.recred.backend.application.RegisterBuffet;
import com.recred.backend.application.RegisterBuffetRequest;
import com.recred.backend.application.RegisterBuffetResponse;
import com.recred.backend.application.RegisterSeller;
import com.recred.backend.application.RegisterSellerRequest;
import com.recred.backend.application.RegisterSellerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SchoolBuffetController {

    private final RegisterBuffet registerBuffetUseCase;
    private final RegisterSeller registerSellerUseCase;
    private final SchoolAccessGuard accessGuard;

    @PostMapping("/colegios/{schoolId}/buffets")
    public ResponseEntity<RegisterBuffetResponse> registerBuffet(
            Authentication authentication,
            @PathVariable UUID schoolId,
            @RequestBody RegisterBuffetRequest request) {
        
        accessGuard.requireSchoolDirectorAccess(authentication, schoolId);

        RegisterBuffetRequest reqWithSchoolId = new RegisterBuffetRequest(
                schoolId,
                request.name(),
                request.habilitationExpirationDate()
        );

        RegisterBuffetResponse response = registerBuffetUseCase.execute(reqWithSchoolId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/buffets/{buffetId}/sellers")
    public ResponseEntity<RegisterSellerResponse> registerSeller(
            Authentication authentication,
            @PathVariable UUID buffetId,
            @RequestBody RegisterSellerRequest request) {
        
        accessGuard.requireBuffetDirectorAccess(authentication, buffetId);

        RegisterSellerRequest reqWithBuffetId = new RegisterSellerRequest(
                buffetId,
                request.username(),
                request.email(),
                request.firstName(),
                request.lastName(),
                request.dni(),
                request.phone(),
                request.cuit()
        );

        RegisterSellerResponse response = registerSellerUseCase.execute(reqWithBuffetId);
        return ResponseEntity.ok(response);
    }
}
