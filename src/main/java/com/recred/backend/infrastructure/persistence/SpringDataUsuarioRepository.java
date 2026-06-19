package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataUsuarioRepository extends
        JpaRepository<UsuarioEntity, UUID> {

    Optional<UsuarioEntity> findByIdCognito(String idCognito);

    Optional<UsuarioEntity> findByEmail(String email);

    @Modifying
    @Query("""
            UPDATE UsuarioEntity u
            SET u.urlFotoPerfil = :urlFotoPerfil
            WHERE u.idCognito = :idCognito
            """)
    void updateFotoPerfil(
            @Param("idCognito") String idCognito,
            @Param("urlFotoPerfil") String urlFotoPerfil
    );

    @Modifying
    @Query("""
            UPDATE UsuarioEntity u
            SET u.urlFotoPerfil = :urlFotoPerfil
            WHERE u.id = :usuarioId
            """)
    void updateFotoPerfilByUsuarioId(
            @Param("usuarioId") java.util.UUID usuarioId,
            @Param("urlFotoPerfil") String urlFotoPerfil
    );

}
