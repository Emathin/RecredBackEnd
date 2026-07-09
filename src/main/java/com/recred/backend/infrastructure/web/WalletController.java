package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetStudentWalletSummary;
import com.recred.backend.application.GetStudentWalletSummaryRequest;
import com.recred.backend.application.LoadBalanceToChild;
import com.recred.backend.application.TransferBalanceBetweenChildren;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final LoadBalanceToChild loadBalanceToChild;
    private final TransferBalanceBetweenChildren transferBalanceBetweenChildren;
    private final GetStudentWalletSummary getStudentWalletSummary;
    private final PlanAccessGuard planAccessGuard;

    public WalletController(
            LoadBalanceToChild loadBalanceToChild, 
            TransferBalanceBetweenChildren transferBalanceBetweenChildren,
            GetStudentWalletSummary getStudentWalletSummary,
            PlanAccessGuard planAccessGuard) {
        this.loadBalanceToChild = loadBalanceToChild;
        this.transferBalanceBetweenChildren = transferBalanceBetweenChildren;
        this.getStudentWalletSummary = getStudentWalletSummary;
        this.planAccessGuard = planAccessGuard;
    }

    private String cognitoSubject(Authentication authentication) {
        if (authentication == null) {
            throw new AuthenticationCredentialsNotFoundException("Usuario no autenticado");
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return validCognitoSubject(jwt.getSubject());
        }
        throw new SecurityException("Autenticación inválida o no es JWT");
    }

    private String validCognitoSubject(String cognitoSubject) {
        if (cognitoSubject == null || cognitoSubject.isBlank()) {
            throw new IllegalArgumentException("Cognito subject cannot be null or blank");
        }
        return cognitoSubject;
    }

    @PostMapping("/load")
    public ResponseEntity<Void> loadBalance(
            @RequestBody LoadBalanceRequest request, 
            Authentication authentication) {
        loadBalanceToChild.execute(
                new com.recred.backend.application.LoadBalanceToChildRequest(
                        cognitoSubject(authentication), 
                        request.studentId(), 
                        request.amount()
                )
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transferBalance(
            @Valid @RequestBody TransferBalanceRequest request,
            Authentication authentication) {
        ensureDifferentStudents(request);
        String cognitoSubject = cognitoSubject(authentication);
        planAccessGuard.requireTutorAdvanced(authentication);
        transferBalanceBetweenChildren.execute(
                new com.recred.backend.application.TransferBalanceBetweenChildrenRequest(
                        cognitoSubject,
                        request.fromStudentId(), 
                        request.toStudentId(), 
                        request.amount()
                )
        );
        return ResponseEntity.ok().build();
    }

    private void ensureDifferentStudents(TransferBalanceRequest request) {
        if (request.fromStudentId().equals(request.toStudentId())) {
            throw new IllegalArgumentException("Cannot transfer to the same student");
        }
    }

    @GetMapping("/students/{studentId}/summary")
    public ResponseEntity<StudentWalletSummaryResponseDTO> getStudentWalletSummary(
            @PathVariable UUID studentId,
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        return ResponseEntity.ok(StudentWalletSummaryResponseDTO.fromApplication(
                getStudentWalletSummary.execute(new GetStudentWalletSummaryRequest(
                        cognitoSubject(authentication),
                        studentId,
                        Optional.ofNullable(from),
                        Optional.ofNullable(to)
                ))
        ));
    }

    public record LoadBalanceRequest(UUID studentId, BigDecimal amount) {}
    public record TransferBalanceRequest(
            @NotNull(message = "fromStudentId cannot be null")
            UUID fromStudentId,
            @NotNull(message = "toStudentId cannot be null")
            UUID toStudentId,
            @NotNull(message = "amount cannot be null")
            @DecimalMin(value = "0.00", inclusive = false,
                    message = "Amount must be greater than zero")
            BigDecimal amount) {}
}
