package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UserService userService;

    public UsuarioController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncUser
            (@AuthenticationPrincipal Jwt jwt,
             @RequestBody(required = false) SyncUserRequest request) {
        Usuario usuario = userService.syncCognitoUser(
                jwt,
                request != null ? request.toProfile() : null
        );
        return ResponseEntity.ok(buildUserResponse(usuario));
    }

    @PatchMapping("/rol")
    public ResponseEntity<Map<String, Object>> assignRole(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> requestBody) {

        String sub = jwt.getSubject();
        String nuevoRol = requestBody.get("rol");

        Usuario usuarioActualizado = userService.updateRole(sub, nuevoRol);

        return ResponseEntity.ok(buildUserResponse(usuarioActualizado));
    }

    private Map<String, Object> buildUserResponse(Usuario usuario) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", usuario.getId());
        response.put("email", usuario.getEmail());
        response.put("nombre", usuario.getNombre());
        response.put("apellido", usuario.getApellido());
        response.put("rol", usuario.getRol() != null ? usuario.getRol().name() : "PENDIENTE");

        if (usuario.getAlumno() != null) {
            response.put("alumnoId", usuario.getAlumno().getId());
        }
        if (usuario.getTutor() != null) {
            response.put("tutorId", usuario.getTutor().getId());
        }
        if (usuario.getKiosquero() != null) {
            response.put("kiosqueroId", usuario.getKiosquero().getId());
            Optional.ofNullable(usuario.getKiosquero().getBuffet())
                    .map(buffet -> buffet.getId())
                    .ifPresent(buffetId -> response.put("buffetId", buffetId));
        }

        return response;
    }
}
