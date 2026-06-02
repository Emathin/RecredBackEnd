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

    public Usuario actualizarPerfil(String cognitoId, String nombre, String apellido,
                                    String telefono, String dni) {
        validarPerfilEditable(nombre, apellido, telefono, dni);

        Usuario usuario = obtenerUsuarioActual(cognitoId);

        if (nombre != null) {
            usuario.setNombre(normalizarCampo("nombre", nombre));
        }

        if (apellido != null) {
            usuario.setApellido(normalizarCampo("apellido", apellido));
        }

        if (telefono != null) {
            usuario.setTelefono(normalizarCampo("telefono", telefono));
        }

        if (dni != null) {
            usuario.setDni(normalizarCampo("dni", dni));
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

    private void validarPerfilEditable(String nombre, String apellido, String telefono,
                                       String dni) {
        if (nombre == null && apellido == null && telefono == null && dni == null) {
            throw new IllegalArgumentException("Debe informar al menos un campo para actualizar");
        }
    }

    private String normalizarCampo(String campo, String valor) {
        String valorNormalizado = valor.trim();

        if (valorNormalizado.isEmpty()) {
            throw new IllegalArgumentException("El campo " + campo + " no puede estar vacio");
        }

        return valorNormalizado;
    }
}
