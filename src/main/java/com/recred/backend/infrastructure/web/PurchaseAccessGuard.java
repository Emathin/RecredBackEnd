package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.exceptions.PurchaseAccessDeniedException;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.ConsumptionRequester;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.repositories.ConsumptionAccessRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PurchaseAccessGuard {

    private final PurchaseRepository purchaseRepository;
    private final ConsumptionAccessRepository consumptionAccessRepository;
    private final KiosqueroAccessGuard kiosqueroAccessGuard;

    public PurchaseAccessGuard(
            PurchaseRepository purchaseRepository,
            ConsumptionAccessRepository consumptionAccessRepository,
            KiosqueroAccessGuard kiosqueroAccessGuard
    ) {
        this.purchaseRepository = purchaseRepository;
        this.consumptionAccessRepository = consumptionAccessRepository;
        this.kiosqueroAccessGuard = kiosqueroAccessGuard;
    }

    public Purchase requirePurchaseAccess(Authentication authentication, UUID purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new PurchaseNotFoundException(purchaseId));
        ConsumptionRequester requester = requester(authentication);

        if (canAccessByStudent(requester, purchase)) {
            requireStudentLicense(purchase);
            return purchase;
        }

        if (requester.role() == RolUsuario.ADMIN
                || requester.role() == RolUsuario.DIRECTIVO_COLEGIO
                || requester.role() == RolUsuario.VENDEDOR) {
            kiosqueroAccessGuard.requireBuffetAccess(authentication, purchase.getBuffetId());
            return purchase;
        }

        throw new PurchaseAccessDeniedException(purchaseId);
    }

    private ConsumptionRequester requester(Authentication authentication) {
        String subject = Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
        return consumptionAccessRepository.findRequesterByCognitoSubject(subject)
                .orElseThrow(() -> new UsuarioAutenticadoNoEncontradoException(subject));
    }

    private boolean canAccessByStudent(ConsumptionRequester requester, Purchase purchase) {
        if (purchase.getStudent() == null || purchase.getStudent().getId() == null) {
            return false;
        }

        UUID studentId = purchase.getStudent().getId();
        return requester.role() == RolUsuario.ALUMNO && studentId.equals(requester.studentId())
                || requester.role() == RolUsuario.PADRE
                && consumptionAccessRepository.tutorCanAccessStudent(
                requester.userId(),
                studentId);
    }

    private void requireStudentLicense(Purchase purchase) {
    }
}
