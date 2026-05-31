package com.recred.backend.infrastructure.web;

import com.recred.backend.config.RequestIdContext;
import com.recred.backend.domain.exceptions.StudentNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SpendingPredictionExceptionHandler {

    @ExceptionHandler(StudentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleStudentNotFound(StudentNotFoundException exception) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("error", exception.getMessage());
        RequestIdContext.currentRequestId()
                .ifPresent(requestId -> response.put("requestId", requestId));
        return response;
    }
}
