package com.recred.backend.unit.application;

import com.recred.backend.application.ResendSellerCredentials;
import com.recred.backend.application.ResendSellerCredentialsRequest;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.CognitoSellerIdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResendSellerCredentialsTest {

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final String USER_ID_COGNITO = "uuid-cognito-1234";

    @Mock
    private KiosqueroRepository kiosqueroRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CognitoSellerIdentityService cognitoService;

    @InjectMocks
    private ResendSellerCredentials useCase;

    private ResendSellerCredentialsRequest request;

    @BeforeEach
    void setUp() {
        request = new ResendSellerCredentialsRequest(SELLER_ID);
    }

    @Test
    void execute_shouldResendCredentialsSuccessfully() {
        givenExistingSellerWithUser();

        whenExecuteIsCalled();

        thenCognitoWasCalledToResendPassword();
    }

    @Test
    void execute_whenSellerNotFound_shouldThrowException() {
        givenNonExistingSeller();

        thenExceptionIsThrownWhenExecuteIsCalled(IllegalArgumentException.class);
    }

    private void givenExistingSellerWithUser() {
        UUID usuarioId = UUID.randomUUID();
        when(kiosqueroRepository.findUsuarioIdById(SELLER_ID)).thenReturn(usuarioId);
        
        Usuario usuario = new Usuario();
        usuario.setIdCognito(USER_ID_COGNITO);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
    }

    private void givenNonExistingSeller() {
        when(kiosqueroRepository.findUsuarioIdById(SELLER_ID)).thenReturn(null);
    }

    private void whenExecuteIsCalled() {
        useCase.execute(request);
    }

    private void thenCognitoWasCalledToResendPassword() {
        verify(cognitoService).resendTemporaryPassword(USER_ID_COGNITO);
    }

    private void thenExceptionIsThrownWhenExecuteIsCalled(
            Class<? extends Exception> expectedException) {
        assertThrows(expectedException, () -> useCase.execute(request));
    }
}
