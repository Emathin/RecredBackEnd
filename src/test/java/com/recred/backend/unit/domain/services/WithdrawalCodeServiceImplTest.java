package com.recred.backend.unit.domain.services;

import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.WithdrawalCodeServiceImpl;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class WithdrawalCodeServiceImplTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Faker faker;

    private WithdrawalCodeServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new WithdrawalCodeServiceImpl(purchaseRepository, faker);
        
        when(faker.name().firstName()).thenReturn("JUAN");
        when(faker.music().instrument()).thenReturn("PIANO");
    }

    @Test
    void shouldGenerateCodeSuccessfullyFirstTry() {
        Purchase purchase = new Purchase();

        when(purchaseRepository.existsByVerificationCodeAndStatusIn(anyString(), anyList()))
                .thenReturn(false);

        String code = service.generateCode(purchase);

        assertNotNull(code);

        assertEquals("JUAN-PIANO", code);
        assertTrue(code.contains("-"), "El código generado debe contener un guion separador");
        assertEquals(code, code.toUpperCase(), "El código debe estar completamente en mayúsculas");
        assertFalse(code.contains(" "), "El código no debe contener espacios en blanco");
        assertTrue(code.matches("^[A-Z0-9]+-[A-Z0-9]+$"),
                "El código debe ser alfanumérico y coincidir con el patrón WORD-WORD");

        verify(purchaseRepository, times(1))
                .existsByVerificationCodeAndStatusIn(anyString(), anyList());
    }

    @Test
    void shouldRetryWhenCodeIsInUse() {
        Purchase purchase = new Purchase();

        when(purchaseRepository.existsByVerificationCodeAndStatusIn(anyString(), anyList()))
                .thenReturn(true)
                .thenReturn(false);

        String code = service.generateCode(purchase);

        assertNotNull(code);

        verify(purchaseRepository, times(2))
                .existsByVerificationCodeAndStatusIn(anyString(), anyList());
    }

    @Test
    void shouldThrowExceptionWhenMaxRetriesExceeded() {
        Purchase purchase = new Purchase();

        when(purchaseRepository.existsByVerificationCodeAndStatusIn(anyString(), anyList()))
                .thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.generateCode(purchase);
        });

        assertTrue(exception.getMessage().contains("No se pudo generar un código único"));

        verify(purchaseRepository, times(11))
                .existsByVerificationCodeAndStatusIn(anyString(), anyList());
    }

    @Test
    void shouldRetryWhenCodeContainsForbiddenWords() {
        Purchase purchase = new Purchase();

        when(faker.music().instrument())
                .thenReturn("Violín")
                .thenReturn("Viola")
                .thenReturn("Corneta")
                .thenReturn("Flauta");

        when(purchaseRepository.existsByVerificationCodeAndStatusIn(anyString(), anyList()))
                .thenReturn(false);

        String code = service.generateCode(purchase);

        assertEquals("JUAN-FLAUTA", code);

        verify(purchaseRepository, times(1))
                .existsByVerificationCodeAndStatusIn(anyString(), anyList());
    }
}