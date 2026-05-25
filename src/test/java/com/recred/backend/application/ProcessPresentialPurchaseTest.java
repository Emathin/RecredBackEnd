package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.services.ValidadorHorarioService;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import com.recred.backend.domain.services.ValidadorPresupuestoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProcessPresentialPurchaseTest {

    @Mock
    private ValidadorHorarioService timeValidator;

    @Mock
    private ValidacionRestriccionesService healthValidator;

    @Mock
    private ValidadorPresupuestoService budgetValidator;

    @InjectMocks
    private ProcessPresentialPurchase processPresentialPurchase;

    @Test
    void execute_LlamaAAmbosValidadoresDeDominio() {
        UUID studentId = UUID.randomUUID();

        Set<Producto> cart = Set.of(new Producto());

        processPresentialPurchase.execute(studentId, cart);

        verify(timeValidator).validarCompra(any(), eq(cart), any(LocalDateTime.class));
        verify(healthValidator).validarCompra(any(), eq(cart));

        verify(budgetValidator).validarPresupuesto(
                eq(studentId),
                eq(java.math.BigDecimal.ZERO),
                eq(java.util.Collections.emptyMap()),
                any(LocalDateTime.class)
        );
    }
}