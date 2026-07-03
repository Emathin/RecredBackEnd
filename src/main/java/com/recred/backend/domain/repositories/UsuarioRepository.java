package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Usuario;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository {
    Optional<Usuario> findById(UUID id);

    Usuario save(Usuario nuevoUsuario);

    Optional<Usuario> findByIdCognito(String sub);

    Optional<Usuario> findByEmail(String email);

    void updateProfile(String idCognito, String nombre, String apellido,
                       String telefono, String dni, LocalDateTime actualizadoEn);

    void updateFotoPerfil(String idCognito, String urlFotoPerfil);

    void updateFotoPerfilByUsuarioId(UUID usuarioId, String urlFotoPerfil);
}
