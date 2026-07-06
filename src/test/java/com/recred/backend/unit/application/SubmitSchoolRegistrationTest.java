package com.recred.backend.unit.application;

import com.recred.backend.application.SubmitSchoolRegistration;
import com.recred.backend.application.SubmitSchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationStatus;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.SchoolRegistrationRequestRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubmitSchoolRegistrationTest {

    @Mock
    private SchoolRegistrationRequestRepository repository;

    @Mock
    private ColegioRepository colegioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private SubmitSchoolRegistration useCase;

    @Test
    void execute_SavesNewRequestWithPendingStatus() {
        SubmitSchoolRegistrationRequest request = new SubmitSchoolRegistrationRequest(
                "School Name",
                "school@test.com",
                "123456789",
                "CUE-123",
                "Director",
                "LastName",
                "director@test.com",
                "987654321",
                "11223344",
                "directorUser123"
        );

        org.mockito.Mockito.when(usuarioRepository.findByEmail("director@test.com"))
                .thenReturn(Optional.empty());

        useCase.execute(request);

        ArgumentCaptor<SchoolRegistrationRequest> captor = 
                ArgumentCaptor.forClass(SchoolRegistrationRequest.class);
        verify(repository).save(captor.capture());

        SchoolRegistrationRequest savedEntity = captor.getValue();
        
        assertEquals("School Name", savedEntity.getSchoolName());
        assertEquals("school@test.com", savedEntity.getSchoolEmail());
        assertEquals("123456789", savedEntity.getSchoolPhone());
        assertEquals("CUE-123", savedEntity.getSchoolCue());
        assertEquals("Director", savedEntity.getDirectorFirstName());
        assertEquals("LastName", savedEntity.getDirectorLastName());
        assertEquals("director@test.com", savedEntity.getDirectorEmail());
        assertEquals("987654321", savedEntity.getDirectorPhone());
        assertEquals("11223344", savedEntity.getDirectorDni());
        assertEquals("directorUser123", savedEntity.getDirectorUsername());
        assertEquals(SchoolRegistrationStatus.PENDING, savedEntity.getStatus());
        assertNotNull(savedEntity.getCreatedAt());
        assertNotNull(savedEntity.getUpdatedAt());
    }
}
