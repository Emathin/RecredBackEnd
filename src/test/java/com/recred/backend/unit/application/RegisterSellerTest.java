package com.recred.backend.unit.application;

import com.recred.backend.application.RegisterSeller;
import com.recred.backend.application.RegisterSellerRequest;
import com.recred.backend.application.RegisterSellerResponse;
import com.recred.backend.domain.exceptions.UsernameExistsException;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.CognitoSellerIdentityService;
import com.recred.backend.domain.services.CreatedCognitoUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterSellerTest {

    private BuffetRepository buffetRepository;
    private UsuarioRepository usuarioRepository;
    private KiosqueroRepository kiosqueroRepository;
    private CognitoSellerIdentityService cognitoService;
    private RegisterSeller registerSeller;

    @BeforeEach
    void setUp() {
        buffetRepository = mock(BuffetRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        kiosqueroRepository = mock(KiosqueroRepository.class);
        cognitoService = mock(CognitoSellerIdentityService.class);
        registerSeller = new RegisterSeller(buffetRepository, usuarioRepository,
                kiosqueroRepository, cognitoService);
    }

    @Test
    void execute_shouldRegisterSellerSuccessfully() {
        UUID buffetId = UUID.randomUUID();
        RegisterSellerRequest req = new RegisterSellerRequest(
                buffetId, "testuser", "test@test.com", "John", "Doe",
                "12345678", "5551234", "20123456789"
        );

        Buffet buffet = new Buffet();
        buffet.setId(buffetId);
        buffet.setColegio(new Colegio());
        when(buffetRepository.findById(buffetId)).thenReturn(Optional.of(buffet));

        when(cognitoService.createSellerUser("testuser", "test@test.com", "John", "Doe"))
                .thenReturn(new CreatedCognitoUser("sub-123", "testuser"));

        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        when(kiosqueroRepository.save(any(Kiosquero.class))).thenAnswer(i -> {
            Kiosquero k = i.getArgument(0);
            k.setId(UUID.randomUUID());
            return k;
        });

        RegisterSellerResponse response = registerSeller.execute(req);

        assertNotNull(response.kiosqueroId());
        assertNotNull(response.usuarioId());
        verify(usuarioRepository).save(any(Usuario.class));
        verify(kiosqueroRepository).save(any(Kiosquero.class));
    }

    @Test
    void execute_shouldRetryCognitoUserCreation_whenUsernameExists() {
        UUID buffetId = UUID.randomUUID();
        RegisterSellerRequest req = new RegisterSellerRequest(
                buffetId, "testuser", "test@test.com", "John", "Doe",
                "12345678", "5551234", "20123456789"
        );

        Buffet buffet = new Buffet();
        buffet.setId(buffetId);
        buffet.setColegio(new Colegio());
        when(buffetRepository.findById(buffetId)).thenReturn(Optional.of(buffet));

        when(cognitoService.createSellerUser("testuser", "test@test.com", "John", "Doe"))
                .thenThrow(new UsernameExistsException("Username exists"));

        when(cognitoService.createSellerUser(anyString(), eq("test@test.com"),
                eq("John"), eq("Doe")))
                .thenReturn(new CreatedCognitoUser("sub-123", "testuser-1234"));

        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        when(kiosqueroRepository.save(any(Kiosquero.class))).thenAnswer(i -> {
            Kiosquero k = i.getArgument(0);
            k.setId(UUID.randomUUID());
            return k;
        });

        RegisterSellerResponse response = registerSeller.execute(req);

        assertNotNull(response.kiosqueroId());
        assertNotNull(response.usuarioId());
    }

    @Test
    void execute_shouldThrowException_whenBuffetDoesNotExist() {
        UUID buffetId = UUID.randomUUID();
        RegisterSellerRequest req = new RegisterSellerRequest(
                buffetId, "testuser", "test@test.com", "John", "Doe",
                "12345678", "5551234", "20123456789"
        );

        when(buffetRepository.findById(buffetId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> registerSeller.execute(req));
    }
}
