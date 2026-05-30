package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetLoggedUserConsumptionDetail;
import com.recred.backend.application.StudentConsumptionCommand;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consumos")
public class StudentConsumptionController {

    private final GetLoggedUserConsumptionDetail getLoggedUserConsumptionDetail;

    public StudentConsumptionController(
            GetLoggedUserConsumptionDetail getLoggedUserConsumptionDetail
    ) {
        this.getLoggedUserConsumptionDetail = getLoggedUserConsumptionDetail;
    }

    @GetMapping
    public StudentConsumptionResponse getConsumption(
            Authentication authentication,
            @RequestParam(name = "alumnoId", required = false) UUID studentId,
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        StudentConsumptionCommand command = new StudentConsumptionCommand(
                Optional.ofNullable(studentId),
                Optional.ofNullable(from),
                Optional.ofNullable(to)
        );
        return StudentConsumptionResponse.fromDomain(
                getLoggedUserConsumptionDetail.execute(
                        cognitoSubject(authentication),
                        command
                )
        );
    }

    private String cognitoSubject(Authentication authentication) {
        if (authentication == null) {
            throw new AuthenticationCredentialsNotFoundException("Usuario no autenticado");
        }

        return authentication.getName();
    }
}
