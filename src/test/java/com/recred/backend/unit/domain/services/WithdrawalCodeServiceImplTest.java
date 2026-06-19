package com.recred.backend.unit.domain.services;

import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.WithdrawalCodeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WithdrawalCodeServiceImplTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private WithdrawalCodeServiceImpl service;

    @BeforeEach
    void setUp() {
        // Inicializamos los mocks antes de cada test
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldGenerateCodeSuccessfullyFirstTry() {
        Purchase purchase = new Purchase();

        // Simulamos que el código generado NO está en uso
        when(purchaseRepository.existsByVerificationCodeAndStatusIn(anyString(), anyList()))
                .thenReturn(false);

        String code = service.generateCode(purchase);

        // Verificamos que el código no sea nulo
        assertNotNull(code);

        // Verificamos el formato esperado (ej. KEVIN-SAXOFON)
        assertTrue(code.contains("-"), "El código generado debe contener un guion separador");
        assertEquals(code, code.toUpperCase(), "El código debe estar completamente en mayúsculas");
        assertFalse(code.contains(" "), "El código no debe contener espacios en blanco");
        assertTrue(code.matches("^[A-Z0-9]+-[A-Z0-9]+$"),
                "El código debe ser alfanumérico y coincidir con el patrón WORD-WORD");

        // Verificamos que la base de datos fue consultada exactamente 1 vez
        verify(purchaseRepository, times(1))
                .existsByVerificationCodeAndStatusIn(anyString(), anyList());
    }

    @Test
    void shouldRetryWhenCodeIsInUse() {
        Purchase purchase = new Purchase();

        // Simulamos que el primer código generado SÍ está en uso, pero el segundo está
        // libre
        when(purchaseRepository.existsByVerificationCodeAndStatusIn(anyString(), anyList()))
                .thenReturn(true) // Intento 1: Falla
                .thenReturn(false); // Intento 2: Pasa

        String code = service.generateCode(purchase);

        assertNotNull(code);

        // Verificamos que la base de datos fue consultada exactamente 2 veces por el
        // bucle do-while
        verify(purchaseRepository, times(2))
                .existsByVerificationCodeAndStatusIn(anyString(), anyList());
    }

    @Test
    void shouldThrowExceptionWhenMaxRetriesExceeded() {
        Purchase purchase = new Purchase();

        // Simulamos el peor escenario: TODOS los códigos generados siempre están en uso
        when(purchaseRepository.existsByVerificationCodeAndStatusIn(anyString(), anyList()))
                .thenReturn(true);

        // Verificamos que al pasarse del límite salte el error esperado
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.generateCode(purchase);
        });

        // Verificamos el mensaje de la excepción para confirmar que es nuestra
        // validación
        assertTrue(exception.getMessage().contains("No se pudo generar un código único"));

        // Verificamos que la base de datos fue consultada 11 veces (el límite que le
        // pusimos al contador)
        verify(purchaseRepository, times(11))
                .existsByVerificationCodeAndStatusIn(anyString(), anyList());
    }
}