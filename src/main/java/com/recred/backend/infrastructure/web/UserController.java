package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.User;
import com.recred.backend.domain.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/perfil")
    public String getProfile(@AuthenticationPrincipal Jwt jwt) {
        String cognitoId = jwt.getSubject();

        String email = jwt.getClaimAsString("email");
        String nombre = jwt.getClaimAsString("given_name");

        return "El usuario autenticado es: " + email + " con ID: " + cognitoId;
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> synchronizeUser(@AuthenticationPrincipal Jwt jwt) {

        User user = userService.synchronizeCognitoUser(jwt);

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("nombre", user.getName());
        response.put("apellido", user.getSurname());

        response.put("rol", user.getRole() != null ? user.getRole().name() : "PENDIENTE");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        String sub = jwt.getSubject();

        User user = userService.getCurrentUSer(sub);

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId()); // UUID interno
        response.put("email", user.getEmail());
        response.put("nombre", user.getName());
        response.put("apellido", user.getSurname());

        response.put("rol", user.getRole().name());

        return ResponseEntity.ok(response);
    }
}