package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.domain.exceptions.AlumnoRequeridoParaConsumoException;
import com.recred.backend.domain.exceptions.ConsumoAlumnoNoAutorizadoException;
import com.recred.backend.domain.exceptions.RangoFechasConsumoInvalidoException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.recred.backend.infrastructure.web.StudentConsumptionExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StudentConsumptionExceptionHandlerTest {

    private final StudentConsumptionExceptionHandler handler =
            new StudentConsumptionExceptionHandler();

    @Test
    void mapRequiredStudentError() {
        Map<String, String> response = handler.handleRequiredStudent(
                new AlumnoRequeridoParaConsumoException()
        );

        assertEquals("Debe indicar el alumno para consultar el consumo.", response.get("error"));
    }

    @Test
    void mapInvalidDateRangeError() {
        Map<String, String> response = handler.handleInvalidDateRange(
                new RangoFechasConsumoInvalidoException(
                        LocalDate.of(2026, 5, 20),
                        LocalDate.of(2026, 5, 1)
                )
        );

        assertEquals(
                "El rango de fechas de consumo es invalido: 2026-05-20 - 2026-05-01",
                response.get("error")
        );
    }

    @Test
    void mapForbiddenStudentError() {
        UUID studentId = UUID.randomUUID();

        Map<String, String> response = handler.handleForbiddenStudent(
                new ConsumoAlumnoNoAutorizadoException(studentId)
        );

        assertEquals(
                "No esta autorizado para consultar el consumo del alumno: " + studentId,
                response.get("error")
        );
    }

    @Test
    void mapAuthenticatedUserNotFoundError() {
        Map<String, String> response = handler.handleAuthenticatedUserNotFound(
                new UsuarioAutenticadoNoEncontradoException("cognito-sub")
        );

        assertEquals(
                "No se encontro el usuario autenticado: cognito-sub",
                response.get("error")
        );
    }

    @Test
    void mapMissingAuthenticationError() {
        Map<String, String> response = handler.handleMissingAuthentication(
                new AuthenticationCredentialsNotFoundException("Usuario no autenticado")
        );

        assertEquals("Usuario no autenticado", response.get("error"));
    }
}

