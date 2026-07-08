package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.SuscripcionUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.SuscripcionColegioRepository;
import com.recred.backend.domain.repositories.SuscripcionUsuarioRepository;
import com.recred.backend.domain.services.UsuarioService;
import com.recred.backend.infrastructure.cloudinary.CloudinaryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {

    private final UsuarioService usuarioService;
    private final CloudinaryService cloudinaryService;
    private final SuscripcionUsuarioRepository suscripcionUsuarioRepository;
    private final SuscripcionColegioRepository suscripcionColegioRepository;
    private final Clock clock;
    private final SchoolLicenseAccessGuard schoolLicenseAccessGuard;

    public UserProfileController(UsuarioService usuarioService,
            CloudinaryService cloudinaryService,
            SuscripcionUsuarioRepository suscripcionUsuarioRepository,
            SuscripcionColegioRepository suscripcionColegioRepository,
            Clock clock,
            SchoolLicenseAccessGuard schoolLicenseAccessGuard) {
        this.usuarioService = usuarioService;
        this.cloudinaryService = cloudinaryService;
        this.suscripcionUsuarioRepository = suscripcionUsuarioRepository;
        this.suscripcionColegioRepository = suscripcionColegioRepository;
        this.clock = clock;
        this.schoolLicenseAccessGuard = schoolLicenseAccessGuard;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyUser(Authentication authentication) {
        String sub = cognitoSubject(authentication);
        Usuario usuario = usuarioService.obtenerUsuarioActual(sub);

        return ResponseEntity.ok(buildUserSessionResponse(usuario));
    }

    @GetMapping("/me/profile")
    public ResponseEntity<Map<String, Object>> getMyProfile(Authentication authentication) {
        String sub = cognitoSubject(authentication);
        Usuario usuario = usuarioService.obtenerUsuarioActual(sub);

        return ResponseEntity.ok(buildUserProfileResponse(usuario));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<Map<String, Object>> updateMyProfile(
            Authentication authentication,
            @RequestBody UpdateUserProfileRequest request) {
        String sub = cognitoSubject(authentication);

        Usuario usuario = usuarioService.updateProfile(
                sub,
                request.firstName(),
                request.lastName(),
                request.phone(),
                request.documentNumber()
        );

        return ResponseEntity.ok(buildUserProfileResponse(usuario));
    }

    @PostMapping(value = "/me/profile/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> subirFotoPerfil(
            Authentication authentication,
            @RequestParam("foto") MultipartFile foto) {
        String sub = cognitoSubject(authentication);
        try {
            String url = cloudinaryService.uploadImage(foto.getBytes(), foto.getOriginalFilename());
            Usuario usuario = usuarioService.actualizarFotoPerfil(sub, url);
            return ResponseEntity.ok(buildUserProfileResponse(usuario));
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error al leer la imagen", e);
        }
    }

    private String cognitoSubject(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario no autenticado"));
    }

    private Map<String, Object> buildUserSessionResponse(Usuario usuario) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", usuario.getId());
        response.put("email", usuario.getEmail());
        response.put("firstName", usuario.getNombre());
        response.put("lastName", usuario.getApellido());
        response.put("role", usuario.getRol() != null ? usuario.getRol().name() : "PENDING");

        return response;
    }

    private Map<String, Object> buildUserProfileResponse(Usuario usuario) {
        Map<String, Object> response = buildUserSessionResponse(usuario);
        response.put("phone", usuario.getTelefono());
        response.put("documentNumber", usuario.getDni());
        response.put("urlFotoPerfil", usuario.getUrlFotoPerfil());
        response.put("plan", usuario.getPlan().name());
        response.put("fechaVencimientoPlan", planExpiration(usuario));
        addSchoolLicenseData(response, usuario);

        return response;
    }

    private LocalDateTime planExpiration(Usuario usuario) {
        if (usuario.getId() == null || usuario.hasFreePlan()) {
            return null;
        }

        return suscripcionUsuarioRepository.findActiveByUsuarioId(usuario.getId())
                .map(SuscripcionUsuario::getFechaVencimiento)
                .orElse(null);
    }

    private void addSchoolLicenseData(Map<String, Object> response, Usuario usuario) {
        if (!isSchoolUser(usuario)) {
            return;
        }

        response.put(
                "estadoLicenciaColegio",
                schoolLicenseAccessGuard.getLicenseStatusForUser(usuario)
        );
        response.put(
                "fechaVencimientoLicenciaColegio",
                schoolLicenseAccessGuard.getLicenseExpirationForUser(usuario)
        );
    }

    private boolean isSchoolUser(Usuario usuario) {
        return usuario.getRol() == RolUsuario.DIRECTIVO_COLEGIO
                || usuario.getRol() == RolUsuario.ALUMNO
                || usuario.getRol() == RolUsuario.VENDEDOR;
    }
}
