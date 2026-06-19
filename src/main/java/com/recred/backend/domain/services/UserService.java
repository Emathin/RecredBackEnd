package com.recred.backend.domain.services;

import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.Optional;

public class UserService {

    private final UsuarioRepository usuarioRepository;

    public UserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario syncCognitoUser(Jwt jwt) {
        return syncCognitoUser(jwt, CognitoUserProfile.empty());
    }

    public Usuario syncCognitoUser(Jwt jwt, CognitoUserProfile profile) {
        CognitoUserProfile userProfile =
                profile != null ? profile : CognitoUserProfile.empty();
        String sub = jwt.getSubject();
        String username = firstNonBlank(
                jwt.getClaimAsString("username"),
                jwt.getClaimAsString("cognito:username"),
                jwt.getClaimAsString("preferred_username")
        );
        String email = firstNonBlank(
                jwt.getClaimAsString("email"),
                userProfile.email(),
                emailFallback(username, sub)
        );
        String nombre = firstNonBlank(
                jwt.getClaimAsString("given_name"),
                userProfile.nombre(),
                jwt.getClaimAsString("name"),
                username,
                "Sin Nombre"
        );
        String apellido = firstNonBlank(
                jwt.getClaimAsString("family_name"),
                userProfile.apellido(),
                "Sin Apellido"
        );

        Optional<Usuario> usuarioExistente = usuarioRepository.findByIdCognito(sub);

        if (usuarioExistente.isPresent()) {
            return usuarioExistente.get();
        }

        Usuario nuevoUsuario = new Usuario();

        nuevoUsuario.setIdCognito(sub);
        nuevoUsuario.setEmail(email);
        nuevoUsuario.setNombre(nombre);
        nuevoUsuario.setApellido(apellido);

        nuevoUsuario.setRol(RolUsuario.PADRE);

        nuevoUsuario.setAutenticadoConCognito(true);
        nuevoUsuario.setActivo(true);
        nuevoUsuario.setCreadoEn(LocalDateTime.now());
        nuevoUsuario.setActualizadoEn(LocalDateTime.now());

        return usuarioRepository.save(nuevoUsuario);
    }

    public Usuario getCurrentUser(String cognitoId) {
        return usuarioRepository.findByIdCognito(cognitoId)
                .orElseThrow(() -> new UsuarioAutenticadoNoEncontradoException(cognitoId));
    }

    public Usuario updateProfile(String cognitoId, String firstName, String lastName,
                                 String phone, String documentNumber) {
        validateEditableProfile(firstName, lastName, phone, documentNumber);

        Usuario usuario = getCurrentUser(cognitoId);

        if (firstName != null) {
            usuario.setNombre(normalizeField("firstName", firstName));
        }

        if (lastName != null) {
            usuario.setApellido(normalizeField("lastName", lastName));
        }

        if (phone != null) {
            usuario.setTelefono(normalizeField("phone", phone));
        }

        if (documentNumber != null) {
            usuario.setDni(normalizeField("documentNumber", documentNumber));
        }

        usuario.setActualizadoEn(LocalDateTime.now());

        return usuarioRepository.save(usuario);
    }

    public Usuario updateRole(String cognitoId, String nuevoRol) {
        Usuario usuario = getCurrentUser(cognitoId);

        usuario.setRol(RolUsuario.valueOf(nuevoRol.toUpperCase()));
        usuario.setActualizadoEn(LocalDateTime.now());

        return usuarioRepository.save(usuario);
    }

    public Usuario updateProfilePhoto(String cognitoId, String urlFotoPerfil) {
        usuarioRepository.updateFotoPerfil(cognitoId, urlFotoPerfil);
        Usuario usuario = getCurrentUser(cognitoId);
        usuario.setUrlFotoPerfil(urlFotoPerfil);
        return usuario;
    }

    private void validateEditableProfile(String firstName, String lastName, String phone,
                                         String documentNumber) {
        if (firstName == null && lastName == null && phone == null && documentNumber == null) {
            throw new IllegalArgumentException("At least one field must be provided");
        }
    }

    private String normalizeField(String field, String value) {
        String normalizedValue = value.trim();

        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException("Field " + field + " cannot be empty");
        }

        return normalizedValue;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String emailFallback(String username, String sub) {
        String candidate = firstNonBlank(username, sub);
        if (candidate == null) {
            return "usuario-cognito@recred.local";
        }
        if (candidate.contains("@")) {
            return candidate;
        }
        return candidate + "@cognito.local";
    }
}
