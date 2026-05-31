package com.recred.backend.domain.services;

import com.recred.backend.domain.exceptions.TimeRestrictionException;
import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.TimeRestriction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeValidatorServiceTest {

    @InjectMocks
    private TimeValidatorService validadorService;

    @Mock
    private Student student;

    @Mock
    private TimeRestriction restriccionMock;

    @Mock
    private Product productMock;

    @Test
    void validatePurchase_LanzaExcepcion_SiProductoEstaBloqueado() {
        when(productMock.getName()).thenReturn("Alfajor de Chocolate");

        when(student.getTimeRestrictions()).thenReturn(Set.of(restriccionMock));

        when(restriccionMock.blocks(eq(productMock), any(LocalDateTime.class))).thenReturn(true);

        Set<Product> carrito = Set.of(productMock);
        LocalDateTime ahora = LocalDateTime.now();

        TimeRestrictionException exception = assertThrows(
                TimeRestrictionException.class,
                () -> validadorService.validatePurchase(student, carrito, ahora)
        );

        assertTrue(exception.getMessage().contains("Alfajor de Chocolate"));
        assertTrue(exception.getMessage().contains("bloqueadas en este horario"));
    }

    @Test
    void validatePurchase_PasaExitosamente_SiNingunProductoEstaBloqueado() {
        when(student.getTimeRestrictions()).thenReturn(Set.of(restriccionMock));

        when(restriccionMock.blocks(eq(productMock), any(LocalDateTime.class))).thenReturn(false);

        Set<Product> carrito = Set.of(productMock);
        LocalDateTime ahora = LocalDateTime.now();

        assertDoesNotThrow(() -> validadorService.validatePurchase(student, carrito, ahora));
    }
}