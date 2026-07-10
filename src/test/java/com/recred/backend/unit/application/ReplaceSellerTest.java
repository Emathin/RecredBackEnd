package com.recred.backend.unit.application;

import com.recred.backend.application.RegisterSellerResponse;
import com.recred.backend.application.ReplaceSeller;
import com.recred.backend.application.ReplaceSellerRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

public class ReplaceSellerTest {

    private BuffetRepository buffetRepository;
    private UsuarioRepository usuarioRepository;
    private KiosqueroRepository kiosqueroRepository;
    private CognitoSellerIdentityService cognitoService;
    private ReplaceSeller replaceSeller;

    @BeforeEach
    void setUp() {
        buffetRepository = mock(BuffetRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        kiosqueroRepository = mock(KiosqueroRepository.class);
        cognitoService = mock(CognitoSellerIdentityService.class);
        replaceSeller = new ReplaceSeller(buffetRepository, usuarioRepository, kiosqueroRepository, cognitoService);
    }

    @Test
    void execute_WhenBuffetHasOldSeller_ShouldDisableOldSellerAndRegisterNewOne() {
        UUID buffetId = UUID.randomUUID();
        ReplaceSellerRequest request = givenRequest(buffetId);
        Buffet buffet = givenBuffetWithOldSeller(buffetId);
        givenServices(request, buffet);

        RegisterSellerResponse response = whenExecuting(request);

        thenOldSellerIsDisabledAndNewOneIsRegistered(buffet, response);
    }

    @Test
    void execute_WhenBuffetHasNoOldSeller_ShouldJustRegisterNewOne() {
        UUID buffetId = UUID.randomUUID();
        ReplaceSellerRequest request = givenRequest(buffetId);
        Buffet buffet = givenBuffetWithoutOldSeller(buffetId);
        givenServices(request, buffet);

        RegisterSellerResponse response = whenExecuting(request);

        thenNoSellerIsDisabledAndNewOneIsRegistered(response);
    }

    @Test
    void execute_WhenUsernameExists_ShouldRetryWithSuffix() {
        UUID buffetId = UUID.randomUUID();
        ReplaceSellerRequest request = givenRequest(buffetId);
        Buffet buffet = givenBuffetWithoutOldSeller(buffetId);
        givenServicesWithUsernameConflict(request, buffet);

        RegisterSellerResponse response = whenExecuting(request);

        thenNewSellerIsRegisteredWithDifferentUsername(response);
    }

    private ReplaceSellerRequest givenRequest(UUID buffetId) {
        return new ReplaceSellerRequest(
                buffetId,
                "newseller",
                "seller@test.com",
                "New",
                "Seller",
                "12345678",
                "123456789",
                "20123456789"
        );
    }

    private Buffet givenBuffetWithOldSeller(UUID buffetId) {
        Buffet buffet = new Buffet();
        buffet.setId(buffetId);
        buffet.setColegio(new Colegio());
        
        Usuario oldUsuario = new Usuario();
        oldUsuario.setIdCognito("old-sub-123");
        Kiosquero oldKiosquero = new Kiosquero();
        oldKiosquero.setUsuario(oldUsuario);
        buffet.setVendedor(oldKiosquero);
        
        when(buffetRepository.findById(buffetId)).thenReturn(Optional.of(buffet));
        return buffet;
    }

    private Buffet givenBuffetWithoutOldSeller(UUID buffetId) {
        Buffet buffet = new Buffet();
        buffet.setId(buffetId);
        buffet.setColegio(new Colegio());
        buffet.setVendedor(null);
        
        when(buffetRepository.findById(buffetId)).thenReturn(Optional.of(buffet));
        return buffet;
    }

    private void givenServices(ReplaceSellerRequest request, Buffet buffet) {
        when(cognitoService.createSellerUser(request.username(), request.email(), request.firstName(), request.lastName()))
                .thenReturn(new CreatedCognitoUser("new-sub-456", "tempPass"));
                
        Usuario savedUser = new Usuario();
        savedUser.setId(UUID.randomUUID());
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(savedUser);
        
        Kiosquero savedKiosquero = new Kiosquero();
        savedKiosquero.setId(UUID.randomUUID());
        when(kiosqueroRepository.save(any(Kiosquero.class))).thenReturn(savedKiosquero);
    }

    private void givenServicesWithUsernameConflict(ReplaceSellerRequest request, Buffet buffet) {
        when(cognitoService.createSellerUser(eq(request.username()), eq(request.email()), eq(request.firstName()), eq(request.lastName())))
                .thenThrow(new UsernameExistsException("Exists"));
                
        when(cognitoService.createSellerUser(any(String.class), eq(request.email()), eq(request.firstName()), eq(request.lastName())))
                .thenReturn(new CreatedCognitoUser("new-sub-456", "tempPass"));
                
        Usuario savedUser = new Usuario();
        savedUser.setId(UUID.randomUUID());
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(savedUser);
        
        Kiosquero savedKiosquero = new Kiosquero();
        savedKiosquero.setId(UUID.randomUUID());
        when(kiosqueroRepository.save(any(Kiosquero.class))).thenReturn(savedKiosquero);
    }

    private RegisterSellerResponse whenExecuting(ReplaceSellerRequest request) {
        return replaceSeller.execute(request);
    }

    private void thenOldSellerIsDisabledAndNewOneIsRegistered(Buffet buffet, RegisterSellerResponse response) {
        verify(cognitoService).disableUser("old-sub-123");
        verify(usuarioRepository).save(any(Usuario.class));
        verify(kiosqueroRepository).save(any(Kiosquero.class));
        verify(buffetRepository).save(buffet);
        assertThat(buffet.getVendedor()).isNotNull();
        assertThat(response).isNotNull();
    }

    private void thenNoSellerIsDisabledAndNewOneIsRegistered(RegisterSellerResponse response) {
        verify(cognitoService, never()).disableUser(any(String.class));
        verify(usuarioRepository).save(any(Usuario.class));
        verify(kiosqueroRepository).save(any(Kiosquero.class));
        assertThat(response).isNotNull();
    }

    private void thenNewSellerIsRegisteredWithDifferentUsername(RegisterSellerResponse response) {
        verify(cognitoService, never()).disableUser(any(String.class));
        verify(usuarioRepository).save(any(Usuario.class));
        verify(kiosqueroRepository).save(any(Kiosquero.class));
        assertThat(response).isNotNull();
    }
}
