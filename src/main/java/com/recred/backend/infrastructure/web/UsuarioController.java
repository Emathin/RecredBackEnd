package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.services.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ResponseEntity<Map<String, Object>> sincronizarUsuario
            (@AuthenticationPrincipal Jwt jwt) {
        Usuario usuario = usuarioService.sincronizarUsuarioCognito(jwt);
        return ResponseEntity.ok(construirRespuestaSesionUsuario(usuario));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> obtenerMiUsuario(Authentication authentication) {
        String sub = cognitoSubject(authentication);
        Usuario usuario = usuarioService.obtenerUsuarioActual(sub);

        return ResponseEntity.ok(construirRespuestaSesionUsuario(usuario));
    }

    @GetMapping("/me/perfil")
    public ResponseEntity<Map<String, Object>> obtenerMiPerfil(Authentication authentication) {
        String sub = cognitoSubject(authentication);
        Usuario usuario = usuarioService.obtenerUsuarioActual(sub);

        return ResponseEntity.ok(construirRespuestaPerfilUsuario(usuario));
    }

    @PatchMapping("/me/perfil")
    public ResponseEntity<Map<String, Object>> actualizarMiPerfil(
            Authentication authentication,
            @RequestBody ActualizarPerfilUsuarioRequest request) {
        String sub = cognitoSubject(authentication);

        Usuario usuario = usuarioService.actualizarPerfil(
                sub,
                request.nombre(),
                request.apellido(),
                request.telefono(),
                request.dni()
        );

        return ResponseEntity.ok(construirRespuestaPerfilUsuario(usuario));
    }

    @PatchMapping("/rol")
    public ResponseEntity<Map<String, Object>> asignarRol(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> requestBody) {

        String sub = jwt.getSubject();
        String nuevoRol = requestBody.get("rol");

        Usuario usuarioActualizado = usuarioService.actualizarRol(sub, nuevoRol);

        return ResponseEntity.ok(construirRespuestaSesionUsuario(usuarioActualizado));
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }

    private Map<String, Object> construirRespuestaSesionUsuario(Usuario usuario) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", usuario.getId());
        response.put("email", usuario.getEmail());
        response.put("nombre", usuario.getNombre());
        response.put("apellido", usuario.getApellido());
        response.put("rol", usuario.getRol() != null ? usuario.getRol().name() : "PENDIENTE");

        return response;
    }

    private Map<String, Object> construirRespuestaPerfilUsuario(Usuario usuario) {
        Map<String, Object> response = construirRespuestaSesionUsuario(usuario);
        response.put("telefono", usuario.getTelefono());
        response.put("dni", usuario.getDni());

        return response;
    }
}
