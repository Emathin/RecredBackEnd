package com.recred.backend.unit.application;

import com.recred.backend.application.TutorChildrenPlanPropagator;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TutorChildrenPlanPropagatorTest {

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private TutorChildrenPlanPropagator propagator;
    private UUID tutorUserId;
    private UUID childId;
    private UUID childUserId;
    private Usuario changedTutorUser;
    private Alumno child;
    private Tutor changedTutor;

    @BeforeEach
    void setUp() {
        propagator = new TutorChildrenPlanPropagator(tutorRepository, usuarioRepository);
    }

    @Test
    void givenTutorBuysFirstPlan_whenPropagatingPlanChange_thenAssignedChildrenReceiveTutorPlan() {
        givenTutorWithAssignedChild(TipoPlan.INTERMEDIO);
        givenChangedTutorExists();
        givenStudentTutors(changedTutor);

        whenPropagatingPlanChange();

        thenChildPlanWasUpdatedTo(TipoPlan.INTERMEDIO);
    }

    @Test
    void givenTutorUpgradesPlan_whenPropagatingPlanChange_thenAssignedChildrenReceiveAdvancedPlan() {
        givenTutorWithAssignedChild(TipoPlan.AVANZADO);
        givenChangedTutorExists();
        givenStudentTutors(changedTutor);

        whenPropagatingPlanChange();

        thenChildPlanWasUpdatedTo(TipoPlan.AVANZADO);
    }

    @Test
    void givenAnotherTutorHasBetterPlan_whenPropagatingPlanChange_thenChildReceivesBestTutorPlan() {
        givenTutorWithAssignedChild(TipoPlan.INTERMEDIO);
        Tutor anotherTutor = givenAnotherTutorWithPlan(TipoPlan.AVANZADO);
        givenChangedTutorExists();
        givenStudentTutors(changedTutor, anotherTutor);

        whenPropagatingPlanChange();

        thenChildPlanWasUpdatedTo(TipoPlan.AVANZADO);
    }

    @Test
    void givenTutorReturnsToFreePlan_whenPropagatingPlanChange_thenChildReturnsToFreePlan() {
        givenTutorWithAssignedChild(TipoPlan.GRATUITO);
        givenChangedTutorExists();
        givenStudentTutors(changedTutor);

        whenPropagatingPlanChange();

        thenChildPlanWasUpdatedTo(TipoPlan.GRATUITO);
    }

    @Test
    void givenUserIsNotParent_whenPropagatingPlanChange_thenNoChildPlanIsUpdated() {
        givenVendorUserWithPaidPlan();

        whenPropagatingPlanChange();

        thenTutorWasNotSearched();
        thenNoChildPlanWasUpdated();
    }

    private void givenTutorWithAssignedChild(TipoPlan plan) {
        tutorUserId = UUID.randomUUID();
        childId = UUID.randomUUID();
        childUserId = UUID.randomUUID();
        changedTutorUser = user(tutorUserId, RolUsuario.PADRE, plan);
        child = child(childId, childUserId);
        changedTutor = tutor(changedTutorUser, child);
    }

    private Tutor givenAnotherTutorWithPlan(TipoPlan plan) {
        return tutor(user(UUID.randomUUID(), RolUsuario.PADRE, plan), child);
    }

    private void givenVendorUserWithPaidPlan() {
        changedTutorUser = user(UUID.randomUUID(), RolUsuario.VENDEDOR, TipoPlan.AVANZADO);
    }

    private void givenChangedTutorExists() {
        when(tutorRepository.findByUsuarioId(tutorUserId))
                .thenReturn(Optional.of(changedTutor));
    }

    private void givenStudentTutors(Tutor... tutors) {
        when(tutorRepository.findByAlumnoId(childId)).thenReturn(List.of(tutors));
    }

    private void whenPropagatingPlanChange() {
        propagator.propagatePlanChangeFor(changedTutorUser);
    }

    private void thenChildPlanWasUpdatedTo(TipoPlan plan) {
        verify(usuarioRepository).updatePlan(childUserId, plan);
    }

    private void thenTutorWasNotSearched() {
        verify(tutorRepository, never()).findByUsuarioId(any(UUID.class));
    }

    private void thenNoChildPlanWasUpdated() {
        verify(usuarioRepository, never()).updatePlan(any(UUID.class), any(TipoPlan.class));
    }

    private Usuario user(UUID id, RolUsuario role, TipoPlan plan) {
        Usuario user = new Usuario();
        user.setId(id);
        user.setRol(role);
        user.setPlan(plan);
        return user;
    }

    private Alumno child(UUID id, UUID userId) {
        Alumno child = new Alumno();
        child.setId(id);
        child.setUsuarioId(userId);
        return child;
    }

    private Tutor tutor(Usuario user, Alumno... children) {
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        tutor.setUsuario(user);
        tutor.setHijos(Set.of(children));
        return tutor;
    }
}
