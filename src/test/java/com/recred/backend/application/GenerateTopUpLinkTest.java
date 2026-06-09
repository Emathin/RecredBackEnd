package com.recred.backend.application;

import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.services.ExternalPaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateTopUpLinkTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private ExternalPaymentService externalPaymentService;

    @InjectMocks
    private GenerateTopUpLink generateTopUpLink;

    @Test
    void execute_GeneraLinkDePago_CuandoAlumnoExiste() {
        UUID studentId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");
        TopUpCommand command = new TopUpCommand(studentId, amount);
        String expectedUrl = "https://link.pago";

        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(externalPaymentService.createTopUpPreference(studentId, amount)).thenReturn(expectedUrl);

        String result = generateTopUpLink.execute(command);

        assertEquals(expectedUrl, result);
    }

    @Test
    void execute_LanzaExcepcion_CuandoAlumnoNoExiste() {
        UUID studentId = UUID.randomUUID();
        TopUpCommand command = new TopUpCommand(studentId, new BigDecimal("100.00"));

        when(alumnoRepository.existsById(studentId)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> generateTopUpLink.execute(command));
    }
}
