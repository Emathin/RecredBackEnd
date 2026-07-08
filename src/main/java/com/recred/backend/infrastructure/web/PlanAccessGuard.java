package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.exceptions.UsuarioNoEncontradoException;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PlanAccessGuard {

    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;

    public PlanAccessGuard(
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.tutorRepository = tutorRepository;
    }

    public void requireAtLeast(UUID usuarioId, TipoPlan minimumPlan) {
        requireAtLeast(usuario(usuarioId), minimumPlan);
    }

    public void requireVendorIntermediate(Authentication authentication) {
        requireVendorAtLeast(authentication, TipoPlan.INTERMEDIO);
    }

    public void requireVendorAdvanced(Authentication authentication) {
        requireVendorAtLeast(authentication, TipoPlan.AVANZADO);
    }

    public void requireTutorIntermediate(Authentication authentication) {
        requireTutorAtLeast(authentication, TipoPlan.INTERMEDIO);
    }

    public void requireTutorAdvanced(Authentication authentication) {
        requireTutorAtLeast(authentication, TipoPlan.AVANZADO);
    }

    public void requireStudentInheritedTutorIntermediate(Authentication authentication) {
        requireStudentInheritedTutorAtLeast(authentication, TipoPlan.INTERMEDIO);
    }

    public void requireStudentInheritedTutorAdvanced(Authentication authentication) {
        requireStudentInheritedTutorAtLeast(authentication, TipoPlan.AVANZADO);
    }

    public void requireTutorOrStudentIntermediate(Authentication authentication) {
        requireTutorOrStudentAtLeast(authentication, TipoPlan.INTERMEDIO);
    }

    public void requireTutorOrStudentAdvanced(Authentication authentication) {
        requireTutorOrStudentAtLeast(authentication, TipoPlan.AVANZADO);
    }

    public void requireTutorOrStudentIntermediateForStudent(
            Authentication authentication,
            UUID studentId
    ) {
        requireTutorOrStudentAtLeastForStudent(authentication, studentId, TipoPlan.INTERMEDIO);
    }

    public void requireUserOrInheritedTutorIntermediate(UUID userId) {
        requireUserOrInheritedTutorAtLeast(userId, TipoPlan.INTERMEDIO);
    }

    public void requireUserOrInheritedTutorAdvanced(UUID userId) {
        requireUserOrInheritedTutorAtLeast(userId, TipoPlan.AVANZADO);
    }

    public void requireVendorAtLeast(Authentication authentication, TipoPlan minimumPlan) {
        Usuario usuario = authenticatedUser(authentication);
        if (usuario.getRol() == RolUsuario.ADMIN) {
            return;
        }

        if (usuario.getRol() != RolUsuario.VENDEDOR) {
            throw new AccessDeniedException("El usuario no tiene rol VENDEDOR");
        }

        requireAtLeast(usuario, minimumPlan);
    }

    public void requireTutorAtLeast(Authentication authentication, TipoPlan minimumPlan) {
        Usuario usuario = authenticatedUser(authentication);
        if (usuario.getRol() == RolUsuario.ADMIN) {
            return;
        }

        if (usuario.getRol() != RolUsuario.PADRE) {
            throw new AccessDeniedException("El usuario no tiene rol PADRE");
        }

        requireAtLeast(usuario, minimumPlan);
    }

    private void requireAtLeast(Usuario usuario, TipoPlan minimumPlan) {
        if (!usuario.getPlan().atLeast(minimumPlan)) {
            throw new AccessDeniedException(
                    "El plan " + minimumPlan + " o superior es requerido");
        }
    }

    public void requireAdvanced(UUID usuarioId) {
        requireAtLeast(usuarioId, TipoPlan.AVANZADO);
    }

    private void requireStudentInheritedTutorAtLeast(
            Authentication authentication,
            TipoPlan minimumPlan
    ) {
        Usuario usuario = authenticatedUser(authentication);
        if (usuario.getRol() == RolUsuario.ADMIN) {
            return;
        }

        if (usuario.getRol() != RolUsuario.ALUMNO) {
            throw new AccessDeniedException("El usuario no tiene rol ALUMNO");
        }

        requireTutorForStudentAtLeast(studentId(usuario), minimumPlan);
    }

    private void requireTutorOrStudentAtLeast(
            Authentication authentication,
            TipoPlan minimumPlan
    ) {
        Usuario usuario = authenticatedUser(authentication);
        if (usuario.getRol() == RolUsuario.ADMIN) {
            return;
        }

        if (usuario.getRol() == RolUsuario.PADRE) {
            requireAtLeast(usuario, minimumPlan);
            return;
        }

        if (usuario.getRol() == RolUsuario.ALUMNO) {
            requireTutorForStudentAtLeast(studentId(usuario), minimumPlan);
            return;
        }

        throw new AccessDeniedException("El usuario no tiene rol PADRE o ALUMNO");
    }

    private void requireTutorOrStudentAtLeastForStudent(
            Authentication authentication,
            UUID studentId,
            TipoPlan minimumPlan
    ) {
        Usuario usuario = authenticatedUser(authentication);
        if (usuario.getRol() == RolUsuario.ADMIN) {
            return;
        }

        if (usuario.getRol() == RolUsuario.PADRE) {
            requireTutorOwnsStudentWithPlan(usuario, studentId, minimumPlan);
            return;
        }

        if (usuario.getRol() == RolUsuario.ALUMNO) {
            requireSameStudent(usuario, studentId);
            requireTutorForStudentAtLeast(studentId, minimumPlan);
            return;
        }

        throw new AccessDeniedException("El usuario no tiene rol PADRE o ALUMNO");
    }

    private void requireUserOrInheritedTutorAtLeast(UUID userId, TipoPlan minimumPlan) {
        Usuario usuario = usuario(userId);
        if (usuario.getRol() == RolUsuario.ADMIN) {
            return;
        }

        if (usuario.getRol() == RolUsuario.PADRE) {
            requireAtLeast(usuario, minimumPlan);
            return;
        }

        if (usuario.getRol() == RolUsuario.ALUMNO) {
            requireTutorForStudentAtLeast(studentId(usuario), minimumPlan);
            return;
        }

        throw new AccessDeniedException("El usuario no tiene rol PADRE o ALUMNO");
    }

    private void requireTutorOwnsStudentWithPlan(
            Usuario usuario,
            UUID studentId,
            TipoPlan minimumPlan
    ) {
        Tutor tutor = tutorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "No se encontro un perfil de tutor para este usuario"));
        tutor.childOrThrow(studentId, "El alumno no pertenece al tutor autenticado");
        requireAtLeast(usuario, minimumPlan);
    }

    private void requireTutorForStudentAtLeast(UUID studentId, TipoPlan minimumPlan) {
        boolean allowed = tutorRepository.findByAlumnoId(studentId)
                .stream()
                .map(Tutor::getUsuario)
                .filter(usuario -> usuario != null)
                .anyMatch(usuario -> usuario.getPlan().atLeast(minimumPlan));

        if (!allowed) {
            throw new AccessDeniedException(
                    "El plan " + minimumPlan + " o superior es requerido");
        }
    }

    private void requireSameStudent(Usuario usuario, UUID studentId) {
        if (!studentId(usuario).equals(studentId)) {
            throw new AccessDeniedException("El alumno autenticado no coincide");
        }
    }

    private UUID studentId(Usuario usuario) {
        if (usuario.getAlumno() == null || usuario.getAlumno().getId() == null) {
            throw new AccessDeniedException("El usuario no tiene perfil ALUMNO");
        }

        return usuario.getAlumno().getId();
    }

    private Usuario usuario(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNoEncontradoException(usuarioId));
    }

    private Usuario authenticatedUser(Authentication authentication) {
        String subject = Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
        return usuarioRepository.findByIdCognito(subject)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario autenticado no sincronizado"));
    }
}
