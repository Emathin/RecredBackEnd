package com.recred.backend.unit.application;

import com.recred.backend.application.UpdatePickupSlotRules;
import com.recred.backend.application.UpdatePickupSlotRulesRequest;
import com.recred.backend.application.UpdatePickupSlotRulesResponse;
import com.recred.backend.domain.exceptions.PickupSlotRulesNotFoundException;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePickupSlotRulesTest {

    @Mock
    private FranjaHorariaRepository repository;

    private UpdatePickupSlotRules useCase;

    private UUID slotId;
    private FranjaHoraria slot;
    private UpdatePickupSlotRulesResponse response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new UpdatePickupSlotRules(repository);
    }

    @Test
    void givenTimeSlotExists_whenUpdatingPickupRules_thenUpdatesCapacityAndCutoff() {
        givenPickupSlotRulesScenario();
        givenTimeSlotExists();
        givenTimeSlotCanBeSaved();

        whenUpdatingPickupRules(20, 15);

        thenPickupRulesAreReturned(20, 15);
        thenTimeSlotWasSaved();
    }

    @Test
    void givenCutoffIsMissing_whenUpdatingPickupRules_thenStoresZeroCutoff() {
        givenPickupSlotRulesScenario();
        givenTimeSlotExists();
        givenTimeSlotCanBeSaved();

        whenUpdatingPickupRules(20, null);

        thenPickupRulesAreReturned(20, 0);
    }

    @Test
    void givenNegativeCutoff_whenUpdatingPickupRules_thenFailsBeforeSaving() {
        givenPickupSlotRulesScenario();
        givenTimeSlotExists();

        whenUpdatingPickupRulesFails(20, -1);

        thenPickupRulesAreInvalid();
        thenTimeSlotWasNotSaved();
    }

    @Test
    void givenTimeSlotDoesNotExist_whenUpdatingPickupRules_thenFailsBeforeSaving() {
        givenPickupSlotRulesScenario();
        givenTimeSlotDoesNotExist();

        whenUpdatingPickupRulesFails(20, 15);

        thenPickupSlotRulesWereNotFound();
        thenTimeSlotWasNotSaved();
    }

    private void givenPickupSlotRulesScenario() {
        slotId = UUID.randomUUID();
        slot = new FranjaHoraria();
    }

    private void givenTimeSlotExists() {
        when(repository.findById(slotId)).thenReturn(Optional.of(slot));
    }

    private void givenTimeSlotCanBeSaved() {
        when(repository.save(slot)).thenReturn(slot);
    }

    private void givenTimeSlotDoesNotExist() {
        when(repository.findById(slotId)).thenReturn(Optional.empty());
    }

    private void whenUpdatingPickupRules(Integer capacity, Integer cutoff) {
        response = useCase.execute(new UpdatePickupSlotRulesRequest(slotId, capacity, cutoff));
    }

    private void whenUpdatingPickupRulesFails(Integer capacity, Integer cutoff) {
        thrownException = assertThrows(
                RuntimeException.class,
                () -> whenUpdatingPickupRules(capacity, cutoff));
    }

    private void thenPickupRulesAreReturned(Integer capacity, Integer cutoff) {
        assertEquals(capacity, response.cupoMaximo());
        assertEquals(cutoff, response.minutosCorte());
    }

    private void thenTimeSlotWasSaved() {
        verify(repository).save(slot);
    }

    private void thenPickupRulesAreInvalid() {
        assertTrue(thrownException instanceof IllegalArgumentException);
    }

    private void thenPickupSlotRulesWereNotFound() {
        assertTrue(thrownException instanceof PickupSlotRulesNotFoundException);
    }

    private void thenTimeSlotWasNotSaved() {
        verify(repository, never()).save(any());
    }
}
