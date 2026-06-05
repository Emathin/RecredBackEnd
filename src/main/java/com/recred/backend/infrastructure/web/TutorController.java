package com.recred.backend.infrastructure.web;

import com.recred.backend.application.ObtenerHijosPorPadre;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tutores")
public class TutorController {

    private final ObtenerHijosPorPadre obtenerHijosPorPadre;

    public TutorController(ObtenerHijosPorPadre obtenerHijosPorPadre) {
        this.obtenerHijosPorPadre = obtenerHijosPorPadre;
    }

    @GetMapping("/me/hijos")
    public ResponseEntity<List<StudentDTO>> obtenerMisHijos(Authentication authentication) {
        List<StudentDTO> alumnos = obtenerHijosPorPadre
                .executeByCognitoSubject(cognitoSubject(authentication))
                .stream()
                .map(StudentDTO::fromDomain)
                .toList();

        return ResponseEntity.ok(alumnos);
    }

    @GetMapping("/{tutorId}/hijos")
    public ResponseEntity<List<StudentDTO>> obtenerHijos(@PathVariable UUID tutorId) {
        List<StudentDTO> alumnos = obtenerHijosPorPadre.execute(tutorId)
                .stream()
                .map(StudentDTO::fromDomain)
                .toList();

        return ResponseEntity.ok(alumnos);
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }
}
