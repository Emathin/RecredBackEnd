package com.recred.backend.unit.application;

import com.recred.backend.application.CreateUserSubscriptionPayment;
import com.recred.backend.application.CreateUserSubscriptionPaymentRequest;
import com.recred.backend.application.CreateUserSubscriptionPaymentResponse;
import com.recred.backend.application.SubscriptionPriceCatalog;
import com.recred.backend.domain.exceptions.UserRoleCannotSubscribeException;
import com.recred.backend.domain.exceptions.UsuarioNoEncontradoException;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.ExternalPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUserSubscriptionPaymentTest {

    @Mock
    private UsuarioRepository userRepository;

    @Mock
    private ExternalPaymentService externalPaymentService;

    private CreateUserSubscriptionPayment useCase;

    private UUID userId;
    private CreateUserSubscriptionPaymentResponse response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new CreateUserSubscriptionPayment(
                userRepository,
                externalPaymentService,
                new SubscriptionPriceCatalog()
        );
    }

    @Test
    void givenUserCanSubscribe_whenCreatingSubscriptionPayment_thenCreatesPaymentPreference() {
        givenUserPaymentScenario();
        givenUserExistsWithRole(RolUsuario.PADRE);
        givenPaymentPreferenceUrl();

        whenCreatingUserSubscriptionPayment();

        thenPaymentPreferenceIsReturned();
        thenCatalogPriceIsReturned();
        thenPaymentPreferenceWasCreated();
    }

    @Test
    void givenUserDoesNotExist_whenCreatingSubscriptionPayment_thenFailsBeforeCreatingPayment() {
        givenUserPaymentScenario();
        givenUserDoesNotExist();

        whenCreatingUserSubscriptionPaymentFails();

        thenUserWasNotFound();
        thenPaymentPreferenceWasNotCreated();
    }

    @Test
    void givenUserRoleCannotSubscribe_whenCreatingSubscriptionPayment_thenFails() {
        givenUserPaymentScenario();
        givenUserExistsWithRole(RolUsuario.ALUMNO);

        whenCreatingUserSubscriptionPaymentFails();

        thenUserRoleCannotSubscribe();
        thenPaymentPreferenceWasNotCreated();
    }

    private void givenUserPaymentScenario() {
        userId = UUID.randomUUID();
    }

    private void givenUserExistsWithRole(RolUsuario role) {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(role)));
    }

    private void givenUserDoesNotExist() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
    }

    private void givenPaymentPreferenceUrl() {
        when(externalPaymentService.createUserSubscriptionPreference(
                userId,
                TipoPlan.INTERMEDIO,
                PeriodoSuscripcion.MENSUAL,
                new BigDecimal("5500.00")
        )).thenReturn("https://pay.test/subscription");
    }

    private void whenCreatingUserSubscriptionPayment() {
        response = useCase.execute(request());
    }

    private void whenCreatingUserSubscriptionPaymentFails() {
        thrownException = assertThrows(
                RuntimeException.class,
                this::whenCreatingUserSubscriptionPayment);
    }

    private void thenPaymentPreferenceIsReturned() {
        assertEquals("https://pay.test/subscription", response.paymentUrl());
    }

    private void thenCatalogPriceIsReturned() {
        assertEquals(new BigDecimal("5500.00"), response.price());
        assertEquals("ARS", response.currency());
    }

    private void thenPaymentPreferenceWasCreated() {
        verify(externalPaymentService).createUserSubscriptionPreference(
                userId,
                TipoPlan.INTERMEDIO,
                PeriodoSuscripcion.MENSUAL,
                new BigDecimal("5500.00")
        );
    }

    private void thenUserWasNotFound() {
        assertTrue(thrownException instanceof UsuarioNoEncontradoException);
    }

    private void thenUserRoleCannotSubscribe() {
        assertTrue(thrownException instanceof UserRoleCannotSubscribeException);
    }

    private void thenPaymentPreferenceWasNotCreated() {
        verify(externalPaymentService, never()).createUserSubscriptionPreference(
                any(),
                any(),
                any(),
                any()
        );
    }

    private CreateUserSubscriptionPaymentRequest request() {
        return new CreateUserSubscriptionPaymentRequest(
                userId,
                TipoPlan.INTERMEDIO,
                PeriodoSuscripcion.MENSUAL
        );
    }

    private Usuario user(RolUsuario role) {
        Usuario user = new Usuario();
        user.setId(userId);
        user.setRol(role);
        return user;
    }
}
