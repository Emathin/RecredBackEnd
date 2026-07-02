package com.recred.backend.unit.application;

import com.recred.backend.application.ApproveSchoolRegistration;
import com.recred.backend.application.ApproveSchoolRegistrationRequest;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.Nivel;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SchoolRegistrationRequest;
import com.recred.backend.domain.models.SchoolRegistrationStatus;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.ColegioRepository;
import com.recred.backend.domain.repositories.SchoolRegistrationRequestRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.CognitoDirectorIdentityService;
import com.recred.backend.domain.services.CreatedCognitoUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApproveSchoolRegistrationTest {

    @Mock
    private SchoolRegistrationRequestRepository repository;

    @Mock
    private ColegioRepository colegioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CognitoDirectorIdentityService cognitoService;

    @InjectMocks
    private ApproveSchoolRegistration useCase;

    @Test
    void execute_ApprovesRequestCreatesSchoolAndUser() {
        UUID requestId = UUID.randomUUID();
        SchoolRegistrationRequest requestEntity = new SchoolRegistrationRequest();
        requestEntity.setId(requestId);
        requestEntity.setStatus(SchoolRegistrationStatus.PENDING);
        requestEntity.setSchoolName("Test School");
        requestEntity.setSchoolEmail("test@school.com");
        requestEntity.setSchoolCue("12345");
        requestEntity.setDirectorFirstName("John");
        requestEntity.setDirectorLastName("Doe");
        requestEntity.setDirectorEmail("john@doe.com");
        requestEntity.setDirectorUsername("johndoe123");
        requestEntity.setSchoolLevel(new Nivel());

        when(repository.findById(requestId)).thenReturn(Optional.of(requestEntity));
        
        Colegio savedColegio = new Colegio();
        savedColegio.setId(UUID.randomUUID());
        when(colegioRepository.save(any(Colegio.class))).thenReturn(savedColegio);
        
        CreatedCognitoUser cognitoUser = new CreatedCognitoUser("sub-123", "director-123");
        when(cognitoService.createDirectorUser("johndoe123", "john@doe.com", "John", "Doe")).thenReturn(cognitoUser);

        ApproveSchoolRegistrationRequest request = new ApproveSchoolRegistrationRequest(requestId);
        useCase.execute(request);

        assertEquals(SchoolRegistrationStatus.APPROVED, requestEntity.getStatus());
        verify(repository).save(requestEntity);

        ArgumentCaptor<Colegio> colegioCaptor = ArgumentCaptor.forClass(Colegio.class);
        verify(colegioRepository).save(colegioCaptor.capture());
        Colegio capturedColegio = colegioCaptor.getValue();
        assertEquals("Test School", capturedColegio.getNombre());
        assertEquals("test@school.com", capturedColegio.getEmail());
        assertEquals("12345", capturedColegio.getCue());

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario capturedUsuario = usuarioCaptor.getValue();
        assertEquals("John", capturedUsuario.getNombre());
        assertEquals("Doe", capturedUsuario.getApellido());
        assertEquals("john@doe.com", capturedUsuario.getEmail());
        assertEquals("sub-123", capturedUsuario.getIdCognito());
        assertEquals(RolUsuario.DIRECTIVO_COLEGIO, capturedUsuario.getRol());
        assertEquals(savedColegio, capturedUsuario.getColegio());
    }

    @Test
    void execute_ThrowsException_WhenRequestNotFound() {
        UUID requestId = UUID.randomUUID();
        when(repository.findById(requestId)).thenReturn(Optional.empty());

        ApproveSchoolRegistrationRequest request = new ApproveSchoolRegistrationRequest(requestId);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(request));
    }

    @Test
    void execute_ThrowsException_WhenRequestIsNotPending() {
        UUID requestId = UUID.randomUUID();
        SchoolRegistrationRequest requestEntity = new SchoolRegistrationRequest();
        requestEntity.setId(requestId);
        requestEntity.setStatus(SchoolRegistrationStatus.REJECTED);

        when(repository.findById(requestId)).thenReturn(Optional.of(requestEntity));

        ApproveSchoolRegistrationRequest request = new ApproveSchoolRegistrationRequest(requestId);

        assertThrows(IllegalStateException.class, () -> useCase.execute(request));
    }
}
