package com.recred.backend.infrastructure.web;

import com.recred.backend.application.ObtenerHijosPorPadre;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.services.UsuarioService;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/v1/tutores")
public class TutorController {

    private final ObtenerHijosPorPadre obtenerHijosPorPadre;
    private final UsuarioService usuarioService;

    public TutorController(ObtenerHijosPorPadre obtenerHijosPorPadre, 
                           UsuarioService usuarioService) {
        this.obtenerHijosPorPadre = obtenerHijosPorPadre;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/{tutorId}/hijos")
    public ResponseEntity<List<StudentDTO>> obtenerHijos(
            @PathVariable String tutorId, 
            Authentication authentication) {
        
        UUID id;
        if ("me".equalsIgnoreCase(tutorId)) {
            String cognitoSub = authentication.getName();
            Usuario usuario = usuarioService.obtenerUsuarioActual(cognitoSub);
            if (usuario.getTutor() == null) {
                return ResponseEntity.ok(List.of());
            }
            id = usuario.getTutor().getId();
        } else {
            id = UUID.fromString(tutorId);
        }

        List<StudentDTO> alumnos = obtenerHijosPorPadre.execute(id)
                .stream()
                .map(StudentDTO::fromDomain)
                .toList();

        return ResponseEntity.ok(alumnos);
    }
}