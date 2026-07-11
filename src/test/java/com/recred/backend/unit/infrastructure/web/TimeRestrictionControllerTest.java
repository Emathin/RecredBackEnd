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

    private final UUID studentId = UUID.randomUUID();
    private final UUID timeSlotId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID classificationId = UUID.randomUUID();
    private final UUID restrictionId = UUID.randomUUID();

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
    void givenValidRequestWhenCreatingThenReturnsCreatedRestriction() {
        CreateTimeRestrictionRequest request = givenCreateRequest();
        CreateTimeRestrictionResponse responseMock = givenCreateResponse(request);
        Principal principal = () -> "cognito-sub";
        when(createTimeRestriction.execute(any(CreateTimeRestrictionRequest.class))).thenReturn(responseMock);

        ResponseEntity<CreateTimeRestrictionResponse> response = controller.create(request, principal, authentication);

        thenCreateResponseIsOk(response, responseMock);
    }

    @Test
    void givenStudentIdWhenGettingByStudentThenReturnsRestrictions() {
        GetTimeRestrictionsByStudentResponse responseDto = givenGetByStudentResponse();
        when(getTimeRestrictionsByStudent.execute(new GetTimeRestrictionsByStudentRequest(studentId)))
                .thenReturn(List.of(responseDto));

        ResponseEntity<List<GetTimeRestrictionsByStudentResponse>> response = controller.getByStudent(studentId, authentication);

        thenGetByStudentResponseIsOk(response, responseDto);
    }

    @Test
    void givenValidRequestWhenUpdatingThenReturnsUpdatedRestriction() {
        UpdateTimeRestrictionRequest requestBody = givenUpdateRequest();
        UpdateTimeRestrictionRequest expectedRequest = givenExpectedUpdateRequest(requestBody);
        UpdateTimeRestrictionResponse responseMock = givenUpdateResponse();
        when(updateTimeRestriction.execute(expectedRequest)).thenReturn(responseMock);

        ResponseEntity<UpdateTimeRestrictionResponse> response = controller.update(restrictionId, requestBody, authentication);

        thenUpdateResponseIsOk(response, responseMock);
    }

    @Test
    void givenRestrictionIdWhenDisablingThenReturnsNoContent() {
        ResponseEntity<Void> response = controller.disable(restrictionId, authentication);

        thenDisableResponseIsNoContent(response);
    }

    private CreateTimeRestrictionRequest givenCreateRequest() {
        return new CreateTimeRestrictionRequest(studentId, timeSlotId, categoryId, classificationId, null);
    }

    private CreateTimeRestrictionResponse givenCreateResponse(CreateTimeRestrictionRequest request) {
        return new CreateTimeRestrictionResponse(restrictionId, request.studentId(), request.timeSlotId(),
                request.categoryId(), request.classificationId(), true, UUID.randomUUID());
    }

    private GetTimeRestrictionsByStudentResponse givenGetByStudentResponse() {
        return new GetTimeRestrictionsByStudentResponse(restrictionId, studentId, null, null, null, true, UUID.randomUUID());
    }

    private UpdateTimeRestrictionRequest givenUpdateRequest() {
        return new UpdateTimeRestrictionRequest(null, timeSlotId, categoryId, classificationId);
    }

    private UpdateTimeRestrictionRequest givenExpectedUpdateRequest(UpdateTimeRestrictionRequest requestBody) {
        return new UpdateTimeRestrictionRequest(restrictionId, requestBody.timeSlotId(), requestBody.categoryId(), requestBody.classificationId());
    }

    private UpdateTimeRestrictionResponse givenUpdateResponse() {
        return new UpdateTimeRestrictionResponse(restrictionId, timeSlotId, null, null, null, true, UUID.randomUUID());
    }

    private void thenCreateResponseIsOk(ResponseEntity<CreateTimeRestrictionResponse> response, CreateTimeRestrictionResponse responseMock) {
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(responseMock, response.getBody());
    }

    private void thenGetByStudentResponseIsOk(ResponseEntity<List<GetTimeRestrictionsByStudentResponse>> response, GetTimeRestrictionsByStudentResponse responseDto) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(responseDto), response.getBody());
    }

    private void thenUpdateResponseIsOk(ResponseEntity<UpdateTimeRestrictionResponse> response, UpdateTimeRestrictionResponse responseMock) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseMock, response.getBody());
    }

    private void thenDisableResponseIsNoContent(ResponseEntity<Void> response) {
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(disableTimeRestriction).execute(new DisableTimeRestrictionRequest(restrictionId));
    }
}

