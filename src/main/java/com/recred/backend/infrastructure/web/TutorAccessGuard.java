package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class TutorAccessGuard {

    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final RestriccionHorariaRepository restriccionHorariaRepository;

    public TutorAccessGuard(
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            PresupuestoRepository presupuestoRepository,
            RestriccionHorariaRepository restriccionHorariaRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.tutorRepository = tutorRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.restriccionHorariaRepository = restriccionHorariaRepository;
    }

    public void requireTutorAccess(Authentication authentication) {
        Usuario usuario = authenticatedUser(authentication);
        if (usuario.getRol() == RolUsuario.ADMIN) {
            return;
        }
        if (usuario.getRol() != RolUsuario.PADRE) {
            throw new AccessDeniedException("El usuario no tiene rol PADRE/Tutor");
        }
    }

    public void requireStudentAccess(Authentication authentication, UUID studentId) {
        Usuario usuario = authenticatedUser(authentication);
        if (usuario.getRol() == RolUsuario.ADMIN) {
            return;
        }

        // Si es ALUMNO, debe coincidir con su propio ID
        if (usuario.getRol() == RolUsuario.ALUMNO) {
            if (usuario.getAlumno() != null && usuario.getAlumno().getId().equals(studentId)) {
                return;
            }
            throw new AccessDeniedException("No tiene permisos sobre este alumno");
        }

        // Si es PADRE, debe ser uno de sus hijos
        if (usuario.getRol() == RolUsuario.PADRE) {
            Tutor tutor = tutorRepository.findByUsuarioId(usuario.getId())
                    .orElseThrow(() -> new AccessDeniedException(
                            "No se encontró un perfil de tutor para este usuario"));

            boolean isOwnChild = tutor.getHijos().stream()
                    .anyMatch(child -> child.getId().equals(studentId));
            if (isOwnChild) {
                return;
            }
            throw new AccessDeniedException("No tiene permisos sobre este alumno");
        }

        throw new AccessDeniedException("Rol no autorizado para acceder a este alumno");
    }

    public void requireStrictTutorStudentAccess(Authentication authentication, UUID studentId) {
        Usuario usuario = authenticatedUser(authentication);
        if (usuario.getRol() == RolUsuario.ADMIN) {
            return;
        }
        if (usuario.getRol() != RolUsuario.PADRE) {
            throw new AccessDeniedException("El usuario no tiene rol PADRE/Tutor");
        }

        Tutor tutor = tutorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "No se encontró un perfil de tutor para este usuario"));

        boolean isOwnChild = tutor.getHijos().stream()
                .anyMatch(child -> child.getId().equals(studentId));
        if (!isOwnChild) {
            throw new AccessDeniedException("No tiene permisos sobre este alumno");
        }
    }

    public void requireBudgetAccess(Authentication authentication, UUID budgetId) {
        Usuario usuario = authenticatedUser(authentication);
        if (usuario.getRol() == RolUsuario.ADMIN) {
            return;
        }
        if (usuario.getRol() != RolUsuario.PADRE) {
            throw new AccessDeniedException("El usuario no tiene rol PADRE/Tutor");
        }

        PresupuestoAlumno presupuesto = presupuestoRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Presupuesto no encontrado"));

        Tutor tutor = tutorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "No se encontró un perfil de tutor para este usuario"));

        boolean isOwnChild = tutor.getHijos().stream()
                .anyMatch(child -> child.getId().equals(presupuesto.getAlumnoId()));
        if (!isOwnChild) {
            throw new AccessDeniedException("No tiene permisos sobre este presupuesto");
        }
    }

    public void requireTimeRestrictionAccess(Authentication authentication, UUID restrictionId) {
        Usuario usuario = authenticatedUser(authentication);
        if (usuario.getRol() == RolUsuario.ADMIN) {
            return;
        }
        if (usuario.getRol() != RolUsuario.PADRE) {
            throw new AccessDeniedException("El usuario no tiene rol PADRE/Tutor");
        }

        RestriccionHoraria restriccion = restriccionHorariaRepository
                .findById(restrictionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Restricción horaria no encontrada"));

        Tutor tutor = tutorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "No se encontró un perfil de tutor para este usuario"));

        boolean isOwnChild = tutor.getHijos().stream()
                .anyMatch(child -> child.getId().equals(restriccion.getAlumnoId()));
        if (!isOwnChild) {
            throw new AccessDeniedException("No tiene permisos sobre esta restricción");
        }
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
