package com.recred.backend.infrastructure.web;

import com.recred.backend.application.EnableSchoolCredit;
import com.recred.backend.application.GetActiveSchoolCredit;
import com.recred.backend.application.GetLastRecharge;
import com.recred.backend.domain.exceptions.ActiveCreditExistsException;
import com.recred.backend.domain.models.SchoolCredit;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import com.recred.backend.domain.services.UsuarioService;
import com.recred.backend.domain.models.Usuario;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school-credits")
public class SchoolCreditController {

    private final EnableSchoolCredit enableSchoolCredit;
    private final GetLastRecharge getLastRecharge;
    private final GetActiveSchoolCredit getActiveSchoolCredit;
    private final UsuarioService usuarioService;

    public SchoolCreditController(EnableSchoolCredit enableSchoolCredit,
                                  GetLastRecharge getLastRecharge,
                                  GetActiveSchoolCredit getActiveSchoolCredit,
                                  UsuarioService usuarioService) {
        this.enableSchoolCredit = enableSchoolCredit;
        this.getLastRecharge = getLastRecharge;
        this.getActiveSchoolCredit = getActiveSchoolCredit;
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<?> enableCredit(Authentication authentication,
                                          @RequestBody EnableSchoolCreditRequest request) {
        try {
            String cognitoSub = authentication.getName();
            Usuario usuario = usuarioService.obtenerUsuarioActual(cognitoSub);
            if (usuario == null || usuario.getTutor() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Tutor no encontrado para el usuario actual.");
            }
            UUID tutorId = usuario.getTutor().getId();

            SchoolCredit credit = enableSchoolCredit.execute(
                    tutorId,
                    request.getStudentId(),
                    request.getAmount(),
                    request.getInstallments() != null ? request.getInstallments() : 1
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(credit);
        } catch (ActiveCreditExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred.");
        }
    }

    @GetMapping("/alumno/{studentId}/last-recharge")
    public ResponseEntity<BigDecimal> getLastRechargeAmount(@PathVariable UUID studentId) {
        try {
            BigDecimal amount = getLastRecharge.execute(studentId);
            return ResponseEntity.ok(amount);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/alumno/{studentId}/active")
    public ResponseEntity<SchoolCredit> getActiveCredit(@PathVariable UUID studentId) {
        try {
            return getActiveSchoolCredit.execute(studentId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
