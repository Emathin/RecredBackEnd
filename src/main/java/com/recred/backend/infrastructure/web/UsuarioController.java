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
import java.util.Optional;

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
        response.put("plan", usuario.getPlan() != null ? usuario.getPlan().name() : "GRATUITO");
        response.put("urlFotoPerfil", usuario.getUrlFotoPerfil());
        response.put("phone", usuario.getTelefono());
        response.put("documentNumber", usuario.getDni());

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
