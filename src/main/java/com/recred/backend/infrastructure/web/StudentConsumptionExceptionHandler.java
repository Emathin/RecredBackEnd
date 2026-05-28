package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.exceptions.AlumnoRequeridoParaConsumoException;
import com.recred.backend.domain.exceptions.ConsumoAlumnoNoAutorizadoException;
import com.recred.backend.domain.exceptions.RangoFechasConsumoInvalidoException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class StudentConsumptionExceptionHandler {

    @ExceptionHandler(AlumnoRequeridoParaConsumoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleRequiredStudent(RuntimeException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(RangoFechasConsumoInvalidoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidDateRange(RuntimeException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(ConsumoAlumnoNoAutorizadoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleForbiddenStudent(RuntimeException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(UsuarioAutenticadoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleAuthenticatedUserNotFound(RuntimeException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleMissingAuthentication(RuntimeException exception) {
        return Map.of("error", exception.getMessage());
    }
}
