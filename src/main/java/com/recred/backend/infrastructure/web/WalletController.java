package com.recred.backend.infrastructure.web;

import com.recred.backend.application.LoadBalanceToChild;
import com.recred.backend.application.TransferBalanceBetweenChildren;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final LoadBalanceToChild loadBalanceToChild;
    private final TransferBalanceBetweenChildren transferBalanceBetweenChildren;

    public WalletController(
            LoadBalanceToChild loadBalanceToChild, 
            TransferBalanceBetweenChildren transferBalanceBetweenChildren) {
        this.loadBalanceToChild = loadBalanceToChild;
        this.transferBalanceBetweenChildren = transferBalanceBetweenChildren;
    }

    private String cognitoSubject(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        throw new SecurityException("Autenticación inválida o no es JWT");
    }

    @PostMapping("/load")
    public ResponseEntity<Void> loadBalance(
            @RequestBody LoadBalanceRequest request, 
            Authentication authentication) {
        loadBalanceToChild.execute(
                cognitoSubject(authentication), 
                request.studentId(), 
                request.amount()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transferBalance(
            @RequestBody TransferBalanceRequest request, 
            Authentication authentication) {
        transferBalanceBetweenChildren.execute(
                cognitoSubject(authentication), 
                request.fromStudentId(), 
                request.toStudentId(), 
                request.amount()
        );
        return ResponseEntity.ok().build();
    }

    public record LoadBalanceRequest(UUID studentId, BigDecimal amount) {}
    public record TransferBalanceRequest(
            UUID fromStudentId, 
            UUID toStudentId, 
            BigDecimal amount) {}
}
