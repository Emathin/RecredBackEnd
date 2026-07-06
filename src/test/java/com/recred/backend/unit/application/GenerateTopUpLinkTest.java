package com.recred.backend.unit.application;

import com.recred.backend.application.GenerateTopUpLink;
import com.recred.backend.application.GenerateTopUpLinkResponse;
import com.recred.backend.application.GenerateTopUpLinkRequest;
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
        GenerateTopUpLinkRequest command = new GenerateTopUpLinkRequest(studentId, amount);
        String expectedUrl = "https://link.pago";

        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(externalPaymentService.createTopUpPreference(studentId, amount)).thenReturn(expectedUrl);

        GenerateTopUpLinkResponse result = generateTopUpLink.execute(command);

        assertEquals(expectedUrl, result.paymentUrl());
    }

    @Test
    void execute_LanzaExcepcion_CuandoAlumnoNoExiste() {
        UUID studentId = UUID.randomUUID();
        GenerateTopUpLinkRequest command = new GenerateTopUpLinkRequest(
                studentId,
                new BigDecimal("100.00"));

        when(alumnoRepository.existsById(studentId)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> generateTopUpLink.execute(command));
    }
}
