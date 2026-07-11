package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.application.RegisterBuffet;
import com.recred.backend.application.RegisterBuffetRequest;
import com.recred.backend.application.RegisterBuffetResponse;
import com.recred.backend.application.RegisterSeller;
import com.recred.backend.application.RegisterSellerRequest;
import com.recred.backend.application.RegisterSellerResponse;
import com.recred.backend.application.ReplaceSeller;
import com.recred.backend.application.ResendSellerCredentials;
import com.recred.backend.infrastructure.web.SchoolAccessGuard;
import com.recred.backend.infrastructure.web.SchoolBuffetController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolBuffetControllerTest {

    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID BUFFET_ID = UUID.randomUUID();
    private static final UUID KIOSQUERO_ID = UUID.randomUUID();
    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final String COGNITO_SUBJECT = "test-user";

    @Mock private RegisterBuffet registerBuffet;
    @Mock private RegisterSeller registerSeller;
    @Mock private ReplaceSeller replaceSeller;
    @Mock private ResendSellerCredentials resendSellerCredentials;
    @Mock private SchoolAccessGuard accessGuard;
    
    private SchoolBuffetController controller;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        controller = new SchoolBuffetController(
                registerBuffet, registerSeller, replaceSeller, resendSellerCredentials, accessGuard);
        authentication = new TestingAuthenticationToken(COGNITO_SUBJECT, "");
    }

    @Test
    void registerBuffetShouldReturnOkAndExecuteUseCase() {
        RegisterBuffetRequest request = givenRegisterBuffetRequest();
        givenSchoolDirectorAccess(SCHOOL_ID);
        givenBuffetIsRegistered(BUFFET_ID);

        ResponseEntity<RegisterBuffetResponse> response = controller.registerBuffet(authentication, SCHOOL_ID, request);

        thenBuffetIsRegisteredSuccessfully(response, BUFFET_ID);
        thenDelegatesToRegisterBuffet(request);
    }

    @Test
    void registerSellerShouldReturnOkAndExecuteUseCase() {
        RegisterSellerRequest request = givenRegisterSellerRequest();
        givenBuffetDirectorAccess(BUFFET_ID);
        givenSellerIsRegistered(KIOSQUERO_ID, USUARIO_ID);

        ResponseEntity<RegisterSellerResponse> response = controller.registerSeller(authentication, BUFFET_ID, request);

        thenSellerIsRegisteredSuccessfully(response, KIOSQUERO_ID, USUARIO_ID);
        thenDelegatesToRegisterSeller(request);
    }

    private RegisterBuffetRequest givenRegisterBuffetRequest() {
        return new RegisterBuffetRequest(null, "Buffet Test", LocalDate.now().plusYears(1));
    }

    private RegisterSellerRequest givenRegisterSellerRequest() {
        return new RegisterSellerRequest(
                null, "testuser", "test@test.com", "John", "Doe",
                "12345678", "5551234", "20123456789"
        );
    }

    private void givenSchoolDirectorAccess(UUID schoolId) {
        doNothing().when(accessGuard).requireSchoolDirectorAccess(any(), eq(schoolId));
    }

    private void givenBuffetDirectorAccess(UUID buffetId) {
        doNothing().when(accessGuard).requireBuffetDirectorAccess(any(), eq(buffetId));
    }

    private void givenBuffetIsRegistered(UUID buffetId) {
        when(registerBuffet.execute(any(RegisterBuffetRequest.class)))
                .thenReturn(new RegisterBuffetResponse(buffetId));
    }

    private void givenSellerIsRegistered(UUID kiosqueroId, UUID usuarioId) {
        when(registerSeller.execute(any(RegisterSellerRequest.class)))
                .thenReturn(new RegisterSellerResponse(kiosqueroId, usuarioId));
    }

    private void thenBuffetIsRegisteredSuccessfully(
            ResponseEntity<RegisterBuffetResponse> response,
            UUID expectedBuffetId
    ) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedBuffetId, response.getBody().buffetId());
        verify(accessGuard).requireSchoolDirectorAccess(authentication, SCHOOL_ID);
    }

    private void thenSellerIsRegisteredSuccessfully(
            ResponseEntity<RegisterSellerResponse> response,
            UUID expectedKiosqueroId,
            UUID expectedUsuarioId
    ) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedKiosqueroId, response.getBody().kiosqueroId());
        assertEquals(expectedUsuarioId, response.getBody().usuarioId());
        verify(accessGuard).requireBuffetDirectorAccess(authentication, BUFFET_ID);
    }

    private void thenDelegatesToRegisterBuffet(RegisterBuffetRequest request) {
        org.mockito.ArgumentCaptor<RegisterBuffetRequest> captor = org.mockito.ArgumentCaptor.forClass(RegisterBuffetRequest.class);
        verify(registerBuffet).execute(captor.capture());
        assertEquals(SCHOOL_ID, captor.getValue().schoolId());
        assertEquals(request.name(), captor.getValue().name());
        assertEquals(request.habilitationExpirationDate(), captor.getValue().habilitationExpirationDate());
    }

    private void thenDelegatesToRegisterSeller(RegisterSellerRequest request) {
        org.mockito.ArgumentCaptor<RegisterSellerRequest> captor = org.mockito.ArgumentCaptor.forClass(RegisterSellerRequest.class);
        verify(registerSeller).execute(captor.capture());
        assertEquals(BUFFET_ID, captor.getValue().buffetId());
        assertEquals(request.username(), captor.getValue().username());
        assertEquals(request.email(), captor.getValue().email());
        assertEquals(request.firstName(), captor.getValue().firstName());
        assertEquals(request.lastName(), captor.getValue().lastName());
        assertEquals(request.dni(), captor.getValue().dni());
        assertEquals(request.phone(), captor.getValue().phone());
        assertEquals(request.cuit(), captor.getValue().cuit());
    }
}
