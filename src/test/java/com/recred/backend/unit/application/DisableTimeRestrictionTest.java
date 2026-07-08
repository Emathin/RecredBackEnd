package com.recred.backend.unit.application;

import com.recred.backend.application.DisableTimeRestriction;
import com.recred.backend.application.DisableTimeRestrictionRequest;
import com.recred.backend.application.DisableTimeRestrictionResponse;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisableTimeRestrictionTest {

    @Mock
    private RestriccionHorariaRepository repository;

    private DisableTimeRestriction useCase;

    private UUID restrictionId;
    private RestriccionHoraria restriction;
    private DisableTimeRestrictionResponse response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new DisableTimeRestriction(repository);
    }

    @Test
    void givenActiveTimeRestriction_whenDisablingTimeRestriction_thenDisablesAndSavesIt() {
        givenActiveTimeRestriction();
        givenTimeRestrictionExists();

        whenDisablingTimeRestriction();

        thenTimeRestrictionWasDisabled();
        thenTimeRestrictionWasSaved();
        thenEmptyResponseIsReturned();
    }

    @Test
    void givenMissingTimeRestriction_whenDisablingTimeRestriction_thenFailsAndDoesNotSave() {
        givenTimeRestrictionScenario();
        givenTimeRestrictionDoesNotExist();

        whenDisablingTimeRestrictionFails();

        thenDisableFailsWith("Time restriction not found");
        thenTimeRestrictionWasNotSaved();
    }

    private void givenActiveTimeRestriction() {
        givenTimeRestrictionScenario();
        restriction.setActiva(true);
    }

    private void givenTimeRestrictionScenario() {
        restrictionId = UUID.randomUUID();
        restriction = new RestriccionHoraria();
        restriction.setId(restrictionId);
    }

    private void givenTimeRestrictionExists() {
        when(repository.findById(restrictionId)).thenReturn(Optional.of(restriction));
    }

    private void givenTimeRestrictionDoesNotExist() {
        when(repository.findById(restrictionId)).thenReturn(Optional.empty());
    }

    private void whenDisablingTimeRestriction() {
        response = useCase.execute(disableRequest());
    }

    private void whenDisablingTimeRestrictionFails() {
        thrownException = assertThrows(RuntimeException.class, () -> useCase.execute(disableRequest()));
    }

    private void thenTimeRestrictionWasDisabled() {
        assertFalse(restriction.isActiva());
    }

    private void thenTimeRestrictionWasSaved() {
        verify(repository).save(restriction);
    }

    private void thenEmptyResponseIsReturned() {
        assertNotNull(response);
    }

    private void thenDisableFailsWith(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private void thenTimeRestrictionWasNotSaved() {
        verify(repository, never()).save(any());
    }

    private DisableTimeRestrictionRequest disableRequest() {
        return new DisableTimeRestrictionRequest(restrictionId);
    }
}
