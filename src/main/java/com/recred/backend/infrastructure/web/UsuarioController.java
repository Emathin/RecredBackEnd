package com.recred.backend.infrastructure.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    @GetMapping("/perfil")
    public String obtenerPerfil(@AuthenticationPrincipal Jwt jwt) {
        String cognitoId = jwt.getSubject();

        String email = jwt.getClaimAsString("email");
        String nombre = jwt.getClaimAsString("given_name");

        return "El usuario autenticado es: " + email + " con ID: " + cognitoId;
    }
}