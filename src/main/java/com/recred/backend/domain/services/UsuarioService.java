package com.recred.backend.domain.services;

import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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

        nuevoUsuario.setId(UUID.randomUUID());
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
                .orElseThrow(() -> new RuntimeException
                        ("Usuario no encontrado en la base de datos"));
    }

    public Usuario actualizarRol(String cognitoId, String nuevoRol) {
        Usuario usuario = obtenerUsuarioActual(cognitoId);

        usuario.setRol(RolUsuario.valueOf(nuevoRol.toUpperCase()));
        usuario.setActualizadoEn(LocalDateTime.now());

        return usuarioRepository.save(usuario);
    }
}