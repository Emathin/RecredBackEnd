package com.recred.backend.unit.application;

import com.recred.backend.application.RejectSchoolRegistration;
import com.recred.backend.application.RejectSchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationStatus;
import com.recred.backend.domain.repositories.SchoolRegistrationRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RejectSchoolRegistrationTest {

    @Mock
    private SchoolRegistrationRequestRepository repository;

    @InjectMocks
    private RejectSchoolRegistration useCase;

    @Test
    void execute_UpdatesStatusToRejected_WhenRequestIsPending() {
        UUID requestId = UUID.randomUUID();
        SchoolRegistrationRequest requestEntity = new SchoolRegistrationRequest();
        requestEntity.setId(requestId);
        requestEntity.setStatus(SchoolRegistrationStatus.PENDING);

        when(repository.findById(requestId)).thenReturn(Optional.of(requestEntity));

        RejectSchoolRegistrationRequest request = new RejectSchoolRegistrationRequest(requestId);
        useCase.execute(request);

        assertEquals(SchoolRegistrationStatus.REJECTED, requestEntity.getStatus());
        verify(repository).save(requestEntity);
    }

    @Test
    void execute_ThrowsException_WhenRequestNotFound() {
        UUID requestId = UUID.randomUUID();
        when(repository.findById(requestId)).thenReturn(Optional.empty());

        RejectSchoolRegistrationRequest request = new RejectSchoolRegistrationRequest(requestId);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(request));
    }

    @Test
    void execute_ThrowsException_WhenRequestIsNotPending() {
        UUID requestId = UUID.randomUUID();
        SchoolRegistrationRequest requestEntity = new SchoolRegistrationRequest();
        requestEntity.setId(requestId);
        requestEntity.setStatus(SchoolRegistrationStatus.APPROVED);

        when(repository.findById(requestId)).thenReturn(Optional.of(requestEntity));

        RejectSchoolRegistrationRequest request = new RejectSchoolRegistrationRequest(requestId);

        assertThrows(IllegalStateException.class, () -> useCase.execute(request));
    }
}
