package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.exceptions.InvalidSubscriptionPlanException;
import com.recred.backend.domain.exceptions.UserRoleCannotSubscribeException;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.Usuario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioSubscriptionTest {

    @Test
    void canHaveIndividualSubscription_WhenUserIsParent_ShouldReturnTrue() {
        Usuario user = givenUserWithRole(RolUsuario.PADRE);

        boolean result = whenCheckingIndividualSubscriptionAvailability(user);

        assertTrue(result);
    }

    @Test
    void canHaveIndividualSubscription_WhenUserIsSeller_ShouldReturnTrue() {
        Usuario user = givenUserWithRole(RolUsuario.VENDEDOR);

        boolean result = whenCheckingIndividualSubscriptionAvailability(user);

        assertTrue(result);
    }

    @Test
    void validateCanHaveIndividualSubscription_WhenUserIsStudent_ShouldThrowException() {
        Usuario user = givenUserWithRole(RolUsuario.ALUMNO);

        assertThrows(
                UserRoleCannotSubscribeException.class,
                () -> whenValidatingIndividualSubscriptionAvailability(user)
        );
    }

    @Test
    void activatePaidPlan_WhenPlanIsPaid_ShouldUpdatePlan() {
        Usuario user = givenUserWithRole(RolUsuario.PADRE);

        whenActivatingPlan(user, TipoPlan.AVANZADO);

        assertEquals(TipoPlan.AVANZADO, user.getPlan());
    }

    @Test
    void activatePaidPlan_WhenPlanIsFree_ShouldThrowException() {
        Usuario user = givenUserWithRole(RolUsuario.PADRE);

        assertThrows(
                InvalidSubscriptionPlanException.class,
                () -> whenActivatingPlan(user, TipoPlan.GRATUITO)
        );
    }

    @Test
    void returnToFreePlan_ShouldUpdatePlanToFree() {
        Usuario user = givenUserWithRole(RolUsuario.PADRE);
        user.setPlan(TipoPlan.AVANZADO);

        whenReturningToFreePlan(user);

        assertEquals(TipoPlan.GRATUITO, user.getPlan());
        assertFalse(user.hasPaidPlan());
    }

    private Usuario givenUserWithRole(RolUsuario role) {
        Usuario user = new Usuario();
        user.setRol(role);
        return user;
    }

    private boolean whenCheckingIndividualSubscriptionAvailability(Usuario user) {
        return user.canHaveIndividualSubscription();
    }

    private void whenValidatingIndividualSubscriptionAvailability(Usuario user) {
        user.validateCanHaveIndividualSubscription();
    }

    private void whenActivatingPlan(Usuario user, TipoPlan plan) {
        user.activatePaidPlan(plan);
    }

    private void whenReturningToFreePlan(Usuario user) {
        user.returnToFreePlan();
    }
}
