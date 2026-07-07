package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.Usuario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TipoPlanTest {

    @Test
    void hasPaidPlan_WhenPlanIsIntermediate_ShouldReturnTrue() {
        Usuario user = givenUserWithPlan(TipoPlan.INTERMEDIO);

        boolean result = whenCheckingPaidPlan(user);

        assertTrue(result);
    }

    @Test
    void hasPaidPlan_WhenPlanIsAdvanced_ShouldReturnTrue() {
        Usuario user = givenUserWithPlan(TipoPlan.AVANZADO);

        boolean result = whenCheckingPaidPlan(user);

        assertTrue(result);
    }

    @Test
    void hasFreePlan_WhenPlanIsFree_ShouldReturnTrue() {
        Usuario user = givenUserWithPlan(TipoPlan.GRATUITO);

        boolean result = whenCheckingFreePlan(user);

        assertTrue(result);
        assertFalse(user.hasPaidPlan());
    }

    @Test
    void hasFreePlan_WhenPlanIsNull_ShouldReturnTrue() {
        Usuario user = givenUserWithPlan(null);

        boolean result = whenCheckingFreePlan(user);

        assertTrue(result);
        assertFalse(user.hasPaidPlan());
    }

    @Test
    void atLeast_WhenPlanLevelIsHigher_ShouldReturnTrue() {
        TipoPlan plan = TipoPlan.AVANZADO;

        boolean result = whenCheckingMinimumPlan(plan, TipoPlan.INTERMEDIO);

        assertTrue(result);
    }

    @Test
    void atLeast_WhenPlanLevelIsSame_ShouldReturnTrue() {
        TipoPlan plan = TipoPlan.INTERMEDIO;

        boolean result = whenCheckingMinimumPlan(plan, TipoPlan.INTERMEDIO);

        assertTrue(result);
    }

    @Test
    void atLeast_WhenPlanLevelIsLower_ShouldReturnFalse() {
        TipoPlan plan = TipoPlan.GRATUITO;

        boolean result = whenCheckingMinimumPlan(plan, TipoPlan.INTERMEDIO);

        assertFalse(result);
    }

    private Usuario givenUserWithPlan(TipoPlan plan) {
        Usuario user = new Usuario();
        user.setPlan(plan);
        return user;
    }

    private boolean whenCheckingPaidPlan(Usuario user) {
        return user.hasPaidPlan();
    }

    private boolean whenCheckingFreePlan(Usuario user) {
        return user.hasFreePlan();
    }

    private boolean whenCheckingMinimumPlan(TipoPlan plan, TipoPlan minimumPlan) {
        return plan.atLeast(minimumPlan);
    }
}
