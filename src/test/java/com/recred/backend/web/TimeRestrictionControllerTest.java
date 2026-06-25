package com.recred.backend.web;

import com.recred.backend.infrastructure.web.*;

import com.recred.backend.application.CreateTimeRestriction;
import com.recred.backend.application.DisableTimeRestriction;
import com.recred.backend.application.GetTimeRestrictionsByStudent;
import com.recred.backend.application.TimeRestrictionCommand;
import com.recred.backend.application.UpdateTimeRestriction;
import com.recred.backend.domain.models.RestriccionHoraria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeRestrictionControllerTest {

    @Mock
    private CreateTimeRestriction createTimeRestriction;
    @Mock
    private GetTimeRestrictionsByStudent getTimeRestrictionsByStudent;
    @Mock
    private UpdateTimeRestriction updateTimeRestriction;
    @Mock
    private DisableTimeRestriction disableTimeRestriction;

    private TimeRestrictionController controller;

    @BeforeEach
    void setUp() {
        controller = new TimeRestrictionController(
                createTimeRestriction,
                getTimeRestrictionsByStudent,
                updateTimeRestriction,
                disableTimeRestriction
        );
    }

    @Test
    void create_ShouldReturnCreatedRestriction() {
        TimeRestrictionCommand command = command();
        RestriccionHoraria restriction = restriction();
        Principal principal = () -> "cognito-sub";
        when(createTimeRestriction.execute(command, "cognito-sub")).thenReturn(restriction);

        ResponseEntity<RestriccionHoraria> response = controller.create(command, principal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(restriction, response.getBody());
    }

    @Test
    void getByStudent_ShouldReturnRestrictions() {
        UUID studentId = UUID.randomUUID();
        RestriccionHoraria restriction = restriction();
        when(getTimeRestrictionsByStudent.execute(studentId)).thenReturn(List.of(restriction));

        ResponseEntity<List<RestriccionHoraria>> response = controller.getByStudent(studentId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(restriction), response.getBody());
    }

    @Test
    void update_ShouldReturnUpdatedRestriction() {
        UUID restrictionId = UUID.randomUUID();
        TimeRestrictionCommand command = command();
        RestriccionHoraria restriction = restriction();
        when(updateTimeRestriction.execute(restrictionId, command)).thenReturn(restriction);

        ResponseEntity<RestriccionHoraria> response = controller.update(restrictionId, command);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(restriction, response.getBody());
    }

    @Test
    void disable_ShouldReturnNoContent() {
        UUID restrictionId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.disable(restrictionId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(disableTimeRestriction).execute(restrictionId);
    }

    private TimeRestrictionCommand command() {
        return new TimeRestrictionCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
    }

    private RestriccionHoraria restriction() {
        RestriccionHoraria restriction = new RestriccionHoraria();
        restriction.setId(UUID.randomUUID());
        return restriction;
    }
}
