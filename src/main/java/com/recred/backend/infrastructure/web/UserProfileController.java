package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.services.UserService;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {

    private final UserService userService;
    private final CloudinaryService cloudinaryService;

    public UserProfileController(UserService userService,
            CloudinaryService cloudinaryService) {
        this.userService = userService;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyUser(Authentication authentication) {
        String sub = cognitoSubject(authentication);
        Usuario usuario = userService.getCurrentUser(sub);

        return ResponseEntity.ok(buildUserSessionResponse(usuario));
    }

    @GetMapping("/me/profile")
    public ResponseEntity<Map<String, Object>> getMyProfile(Authentication authentication) {
        String sub = cognitoSubject(authentication);
        Usuario usuario = userService.getCurrentUser(sub);

        return ResponseEntity.ok(buildUserProfileResponse(usuario));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<Map<String, Object>> updateMyProfile(
            Authentication authentication,
            @RequestBody UpdateUserProfileRequest request) {
        String sub = cognitoSubject(authentication);

        Usuario usuario = userService.updateProfile(
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
        String url = cloudinaryService.uploadImage(foto);
        Usuario usuario = userService.updateProfilePhoto(sub, url);
        return ResponseEntity.ok(buildUserProfileResponse(usuario));
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

        return response;
    }
}
