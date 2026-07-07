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
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateUserSubscriptionPaymentTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final ExternalPaymentService externalPaymentService =
            mock(ExternalPaymentService.class);
    private final CreateUserSubscriptionPayment useCase = new CreateUserSubscriptionPayment(
            usuarioRepository,
            externalPaymentService,
            new SubscriptionPriceCatalog()
    );

    @Test
    void execute_WhenUserCanSubscribe_ShouldCreatePreferenceWithCatalogPrice() {
        UUID userId = UUID.randomUUID();
        givenUser(user(userId, RolUsuario.PADRE));
        givenPaymentUrl(userId, TipoPlan.INTERMEDIO, PeriodoSuscripcion.MENSUAL);
        CreateUserSubscriptionPaymentRequest request = request(
                userId,
                TipoPlan.INTERMEDIO,
                PeriodoSuscripcion.MENSUAL
        );

        CreateUserSubscriptionPaymentResponse result = whenExecuting(request);

        assertEquals("https://pay.test/subscription", result.paymentUrl());
        assertEquals(new BigDecimal("5500.00"), result.price());
        assertEquals("ARS", result.currency());
        verify(externalPaymentService).createUserSubscriptionPreference(
                userId,
                TipoPlan.INTERMEDIO,
                PeriodoSuscripcion.MENSUAL,
                new BigDecimal("5500.00")
        );
    }

    @Test
    void execute_WhenUserDoesNotExist_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        givenMissingUser(userId);
        CreateUserSubscriptionPaymentRequest request = request(
                userId,
                TipoPlan.INTERMEDIO,
                PeriodoSuscripcion.MENSUAL
        );

        assertThrows(UsuarioNoEncontradoException.class, () -> whenExecuting(request));
        verify(externalPaymentService, never()).createUserSubscriptionPreference(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void execute_WhenUserRoleCannotSubscribe_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        givenUser(user(userId, RolUsuario.ALUMNO));
        CreateUserSubscriptionPaymentRequest request = request(
                userId,
                TipoPlan.INTERMEDIO,
                PeriodoSuscripcion.MENSUAL
        );

        assertThrows(UserRoleCannotSubscribeException.class, () -> whenExecuting(request));
        verify(externalPaymentService, never()).createUserSubscriptionPreference(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private void givenUser(Usuario user) {
        when(usuarioRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    private void givenMissingUser(UUID userId) {
        when(usuarioRepository.findById(userId)).thenReturn(Optional.empty());
    }

    private void givenPaymentUrl(
            UUID userId,
            TipoPlan plan,
            PeriodoSuscripcion period
    ) {
        when(externalPaymentService.createUserSubscriptionPreference(
                userId,
                plan,
                period,
                new BigDecimal("5500.00")
        )).thenReturn("https://pay.test/subscription");
    }

    private CreateUserSubscriptionPaymentResponse whenExecuting(
            CreateUserSubscriptionPaymentRequest request
    ) {
        return useCase.execute(request);
    }

    private CreateUserSubscriptionPaymentRequest request(
            UUID userId,
            TipoPlan plan,
            PeriodoSuscripcion period
    ) {
        return new CreateUserSubscriptionPaymentRequest(userId, plan, period);
    }

    private Usuario user(UUID userId, RolUsuario role) {
        Usuario user = new Usuario();
        user.setId(userId);
        user.setRol(role);
        return user;
    }
}
