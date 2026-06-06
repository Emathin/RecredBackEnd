package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.services.UsuarioService;
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

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncUser
            (@AuthenticationPrincipal Jwt jwt,
             @RequestBody(required = false) SyncUserRequest request) {
        Usuario usuario = usuarioService.sincronizarUsuarioCognito(
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

        Usuario usuarioActualizado = usuarioService.actualizarRol(sub, nuevoRol);

        return ResponseEntity.ok(buildUserResponse(usuarioActualizado));
    }

    private Map<String, Object> buildUserResponse(Usuario usuario) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", usuario.getId());
        response.put("email", usuario.getEmail());
        response.put("nombre", usuario.getNombre());
        response.put("apellido", usuario.getApellido());
        response.put("rol", usuario.getRol() != null ? usuario.getRol().name() : "PENDIENTE");

        return response;
    }
}
