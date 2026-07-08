package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class TutorChildrenPlanPropagator {

    private final TutorRepository tutorRepository;
    private final UsuarioRepository usuarioRepository;

    public TutorChildrenPlanPropagator(
            TutorRepository tutorRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.tutorRepository = tutorRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public void propagatePlanChangeFor(Usuario user) {
        if (isNotParent(user)) {
            return;
        }

        tutorRepository.findByUsuarioId(user.getId())
                .ifPresent(tutor -> propagatePlanToAssignedChildren(tutor, user));
    }

    private boolean isNotParent(Usuario user) {
        return user == null || user.getRol() != RolUsuario.PADRE;
    }

    private void propagatePlanToAssignedChildren(Tutor tutor, Usuario changedTutorUser) {
        if (tutor.getHijos() == null) {
            return;
        }

        tutor.getHijos().forEach(child -> updateChildPlan(child, changedTutorUser));
    }

    private void updateChildPlan(Alumno child, Usuario changedTutorUser) {
        UUID childUserId = childUserId(child);
        if (childUserId == null) {
            return;
        }

        usuarioRepository.updatePlan(
                childUserId,
                bestTutorPlanFor(child.getId(), changedTutorUser));
    }

    private UUID childUserId(Alumno child) {
        if (child.getUsuarioId() != null) {
            return child.getUsuarioId();
        }

        return Optional.ofNullable(child.getUsuario())
                .map(Usuario::getId)
                .orElse(null);
    }

    private TipoPlan bestTutorPlanFor(UUID childId, Usuario changedTutorUser) {
        TipoPlan changedTutorPlan = changedTutorUser.getPlan();
        if (childId == null) {
            return changedTutorPlan;
        }

        return tutorRepository.findByAlumnoId(childId).stream()
                .map(tutor -> planFor(tutor, changedTutorUser))
                .reduce(changedTutorPlan, this::bestPlan);
    }

    private TipoPlan planFor(Tutor tutor, Usuario changedTutorUser) {
        Usuario tutorUser = tutor.getUsuario();
        if (isChangedTutorUser(tutorUser, changedTutorUser)) {
            return changedTutorUser.getPlan();
        }

        return tutorUser != null ? tutorUser.getPlan() : TipoPlan.GRATUITO;
    }

    private boolean isChangedTutorUser(Usuario tutorUser, Usuario changedTutorUser) {
        return tutorUser != null
                && changedTutorUser != null
                && Objects.equals(tutorUser.getId(), changedTutorUser.getId());
    }

    private TipoPlan bestPlan(TipoPlan currentPlan, TipoPlan candidatePlan) {
        if (candidatePlan == null) {
            return currentPlan;
        }

        return candidatePlan.atLeast(currentPlan) ? candidatePlan : currentPlan;
    }
}
