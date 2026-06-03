package com.recred.backend.domain.services;

import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.Optional;

public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario sincronizarUsuarioCognito(Jwt jwt) {
        String sub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");

        String nombre = jwt.getClaimAsString("given_name");
        String apellido = jwt.getClaimAsString("family_name");

        Optional<Usuario> usuarioExistente = usuarioRepository.findByIdCognito(sub);

        if (usuarioExistente.isPresent()) {
            return usuarioExistente.get();
        }

        Usuario nuevoUsuario = new Usuario();

        nuevoUsuario.setIdCognito(sub);
        nuevoUsuario.setEmail(email);
        nuevoUsuario.setNombre(nombre != null ? nombre : "Sin Nombre");
        nuevoUsuario.setApellido(apellido != null ? apellido : "Sin Apellido");

        nuevoUsuario.setRol(RolUsuario.PADRE);


        nuevoUsuario.setAutenticadoConCognito(true);
        nuevoUsuario.setActivo(true);
        nuevoUsuario.setCreadoEn(LocalDateTime.now());
        nuevoUsuario.setActualizadoEn(LocalDateTime.now());

        return usuarioRepository.save(nuevoUsuario);
    }

    public Usuario obtenerUsuarioActual(String cognitoId) {
        return usuarioRepository.findByIdCognito(cognitoId)
                .orElseThrow(() -> new UsuarioAutenticadoNoEncontradoException(cognitoId));
    }

    public Usuario updateProfile(String cognitoId, String firstName, String lastName,
                                 String phone, String documentNumber) {
        validateEditableProfile(firstName, lastName, phone, documentNumber);

        Usuario usuario = obtenerUsuarioActual(cognitoId);

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

    public Usuario actualizarRol(String cognitoId, String nuevoRol) {
        Usuario usuario = obtenerUsuarioActual(cognitoId);

        usuario.setRol(RolUsuario.valueOf(nuevoRol.toUpperCase()));
        usuario.setActualizadoEn(LocalDateTime.now());

        return usuarioRepository.save(usuario);
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
}
