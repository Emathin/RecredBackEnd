package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.infrastructure.web.*;

import com.recred.backend.application.CreateTimeRestriction;
import com.recred.backend.application.CreateTimeRestrictionRequest;
import com.recred.backend.application.CreateTimeRestrictionResponse;
import com.recred.backend.application.DisableTimeRestriction;
import com.recred.backend.application.DisableTimeRestrictionRequest;
import com.recred.backend.application.GetTimeRestrictionsByStudent;
import com.recred.backend.application.GetTimeRestrictionsByStudentRequest;
import com.recred.backend.application.GetTimeRestrictionsByStudentResponse;
import com.recred.backend.application.UpdateTimeRestriction;
import com.recred.backend.application.UpdateTimeRestrictionRequest;
import com.recred.backend.application.UpdateTimeRestrictionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

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
    @Mock
    private TutorAccessGuard tutorAccessGuard;
    @Mock
    private Authentication authentication;

    private TimeRestrictionController controller;

    @BeforeEach
    void setUp() {
        controller = new TimeRestrictionController(
                createTimeRestriction,
                getTimeRestrictionsByStudent,
                updateTimeRestriction,
                disableTimeRestriction,
                tutorAccessGuard
        );
    }

    @Test
    void create_ShouldReturnCreatedRestriction() {
        CreateTimeRestrictionRequest request = new CreateTimeRestrictionRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null
        );
        CreateTimeRestrictionResponse responseMock = new CreateTimeRestrictionResponse(
                UUID.randomUUID(),
                request.studentId(),
                request.timeSlotId(),
                request.categoryId(),
                request.classificationId(),
                true,
                UUID.randomUUID()
        );
        Principal principal = () -> "cognito-sub";
        when(createTimeRestriction.execute(any(CreateTimeRestrictionRequest.class))).thenReturn(responseMock);

        ResponseEntity<CreateTimeRestrictionResponse> response = controller.create(request, principal, authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(responseMock, response.getBody());
    }

    @Test
    void getByStudent_ShouldReturnRestrictions() {
        UUID studentId = UUID.randomUUID();
        GetTimeRestrictionsByStudentResponse responseDto = new GetTimeRestrictionsByStudentResponse(
                UUID.randomUUID(),
                studentId,
                null,
                null,
                null,
                true,
                UUID.randomUUID()
        );
        when(getTimeRestrictionsByStudent.execute(new GetTimeRestrictionsByStudentRequest(studentId)))
                .thenReturn(List.of(responseDto));

        ResponseEntity<List<GetTimeRestrictionsByStudentResponse>> response = controller.getByStudent(studentId, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(responseDto), response.getBody());
    }

    @Test
    void update_ShouldReturnUpdatedRestriction() {
        UUID restrictionId = UUID.randomUUID();
        UpdateTimeRestrictionRequest requestBody = new UpdateTimeRestrictionRequest(
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        UpdateTimeRestrictionRequest expectedRequest = new UpdateTimeRestrictionRequest(
                restrictionId,
                requestBody.timeSlotId(),
                requestBody.categoryId(),
                requestBody.classificationId()
        );
        UpdateTimeRestrictionResponse responseMock = new UpdateTimeRestrictionResponse(
                restrictionId,
                UUID.randomUUID(),
                null,
                null,
                null,
                true,
                UUID.randomUUID()
        );
        when(updateTimeRestriction.execute(expectedRequest)).thenReturn(responseMock);

        ResponseEntity<UpdateTimeRestrictionResponse> response = controller.update(restrictionId, requestBody, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseMock, response.getBody());
    }

    @Test
    void disable_ShouldReturnNoContent() {
        UUID restrictionId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.disable(restrictionId, authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(disableTimeRestriction).execute(new DisableTimeRestrictionRequest(restrictionId));
    }
}

